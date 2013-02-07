package org.lttng.studio.reader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.eclipse.linuxtools.ctf.core.trace.CTFTraceReader;
import org.eclipse.linuxtools.ctf.core.trace.StreamInputReader;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.request.TmfDataRequest;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.reader.handler.ITraceEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerBase;

public class TraceReader {

	class MinMax {
		public long min;
		public long max;
	}

	private final ModelRegistry registry;
	private final Map<Class<?>, ITraceEventHandler> handlers;
	private final Map<String, TreeSet<TraceHook>> eventHookMap;
	private final TreeSet<TraceHook> catchAllHook;
	private static Class<?>[] argTypes = new Class<?>[] { TraceReader.class, CtfTmfEvent.class };
	private ITmfTrace trace;
	private long now;
	private boolean cancel;
	private int nbCpus;
	private Exception exception;
	private TmfDataRequest request;
	private TmfTimeRange timeRange;

	public TraceReader() {
		handlers = new HashMap<Class<?>, ITraceEventHandler>();
		eventHookMap = new HashMap<String, TreeSet<TraceHook>>();
		catchAllHook = new TreeSet<TraceHook>();
		registry = new ModelRegistry();
	}

	public void registerHook(ITraceEventHandler handler, TraceHook hook) {
		String methodName;
		if (hook.isAllEvent()) {
			methodName = "handle_all_event";
		} else {
			methodName = "handle_" + hook.eventName;
		}
		boolean isHookOk = true;
		TreeSet<TraceHook> eventHooks;
		hook.instance = handler;
		try {
			hook.method = handler.getClass().getMethod(methodName, argTypes);
		} catch (SecurityException e) {
			e.printStackTrace();
			isHookOk = false;
		} catch (NoSuchMethodException e) {
			System.err.println("Error: hook " + handler.getClass() + "." + methodName + " doesn't exist, disabling");
			isHookOk = false;
		}
		if (!isHookOk)
			return;

		if(hook.isAllEvent()) {
			catchAllHook.add(hook);
		} else {
			eventHooks = eventHookMap.get(hook.eventName);
			if (eventHooks == null) {
				eventHooks = new TreeSet<TraceHook>();
				eventHookMap.put(hook.eventName, eventHooks);
			}
			eventHooks.add(hook);
		}
	}

	public void register(ITraceEventHandler handler) {
		if (handler == null)
			return;
		Set<TraceHook> handlerHooks = handler.getHooks();

		/* If handlerHooks is null then add no hooks */
		if (handlerHooks == null || handlerHooks.size() == 0) {
			return;
		}

		/* register individual hooks */
		for (TraceHook hook: handlerHooks) {
			registerHook(handler, hook);
		}

		handlers.put(handler.getClass(), handler);
	}

	public void registerAll(Collection<ITraceEventHandler> handlers) {
		for (ITraceEventHandler handler: handlers) {
			register(handler);
		}
	}

	public void process() throws Exception {
		process(new DummyTimeListener());
	}

	public void process(final TimeListener listener) throws Exception {
		cancel = false;
		if (trace == null)
			throw new RuntimeException("Trace can't be null");

		trace.seekEvent(Integer.MAX_VALUE);
		timeRange = trace.getTimeRange();
		listener.begin(timeRange.getStartTime().getValue(), timeRange.getEndTime().getValue());

		for(ITraceEventHandler handler: handlers.values()) {
			if (cancel == true)
				break;
			handler.handleInit(this);
		}
		// Re-throw any handler exception
		if (exception != null)
			throw exception;

		request = new TmfDataRequest(ITmfEvent.class) {
			@Override
			public void handleData(final ITmfEvent event) {
				if (cancel == true || exception != null || listener.isCanceled())
					request.cancel();
				if (event instanceof CtfTmfEvent) {
					CtfTmfEvent ctf = (CtfTmfEvent) event;
					now = ctf.getTimestamp().getValue();
					listener.progress(now);
					String evName = ctf.getEventName();
					TreeSet<TraceHook> treeSet = eventHookMap.get(evName);
					if (treeSet != null)
						TraceReader.this.runHookSet(treeSet, ctf);
					TraceReader.this.runHookSet(catchAllHook, ctf);
				}
			}
			@Override
			public void handleCancel() {
				cancel = true;
			}
			@Override
			public void handleSuccess() {
			}
			@Override
			public void handleFailure() {
				cancel = true;
			}
		};
		trace.sendRequest(request);
		request.waitForCompletion();

		// Re-throw any handler exception
		if (exception != null)
			throw exception;

		for(ITraceEventHandler handler: handlers.values()) {
			handler.handleComplete(this);
		}

		listener.finished();
	}

	public void runHookSet(TreeSet<TraceHook> hooks, CtfTmfEvent event) {
		for (TraceHook h: hooks){
			if (cancel == true)
				break;
			try {
				h.method.invoke(h.instance, this, event);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				cancel = true;
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				cancel = true;
			} catch (InvocationTargetException e) {
				System.err.println("error while executing " + h.method + " on " + h.instance);
				e.printStackTrace();
				cancel = true;
			}
		}
	}

	public ITraceEventHandler getHandler(
			Class<? extends TraceEventHandlerBase> klass) {
		return handlers.get(klass);
	}

	public void cancel() {
		this.cancel = true;
	}
	public void cancel(Exception e) {
		this.cancel = true;
		this.exception = e;
	}

	public Boolean isCancel() {
		return this.cancel;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public int getNumCpuFromCtfTrace(CtfTmfTrace ctf) {
		int cpus = 0;
		CTFTraceReader reader = new CTFTraceReader(ctf.getCTFTrace());
		Field field;
		Vector<StreamInputReader> v;
		try {
			field = reader.getClass().getDeclaredField("streamInputReaders");
			field.setAccessible(true);
			v = (Vector) field.get(reader);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error trying to retreive the number of CPUs of the trace");
		}
		for (StreamInputReader input: v) {
			cpus = Math.max(cpus, input.getCPU() + 1);
		}
		return cpus;
	}

	public void updateNbCpus() {
		int max = 0;
		if (trace instanceof CtfTmfTrace) {
			CtfTmfTrace ctf = (CtfTmfTrace) trace;
			max = getNumCpuFromCtfTrace(ctf);
		} else if (trace instanceof TmfExperiment) {
			TmfExperiment exp = (TmfExperiment) trace;
			for (ITmfTrace t: exp.getTraces()) {
				if (t instanceof CtfTmfTrace) {
					max = Math.max(max, getNumCpuFromCtfTrace((CtfTmfTrace)t));
				}
			}
		}
		setNbCpus(max);
	}

	public void setTrace(ITmfTrace trace) {
		this.trace = trace;
		updateNbCpus();
	}

	public void setTrace(File file) throws TmfTraceException, IOException {
		CtfTmfTrace ctfTrace = new CtfTmfTrace();
		ctfTrace.initTrace(null, file.getCanonicalPath(), ITmfEvent.class);
		setTrace(ctfTrace);
	}

	public int getNumCpus() {
		return nbCpus;
	}

	public void setNbCpus(int nbCpus) {
		this.nbCpus = nbCpus;
	}

	public void clearHandlers() {
		this.handlers.clear();
		this.eventHookMap.clear();
		this.catchAllHook.clear();
	}

	/* CtfTmfEvent already contains the offset
	public static long clockTime(EventDefinition event) {
		return (Long) event.getStreamInputReader()
							.getParent()
							.getTrace()
							.getClock()
							.getProperty("offset") + event.getTimestamp();
	}
	*/

	public ModelRegistry getRegistry() {
		return registry;
	}

	public long getCurrentTime() {
		return now;
	}

	public static TmfExperiment makeTmfExperiment(ITmfTrace[] traceSet) {
		return new TmfExperiment(ITmfEvent.class, "none", traceSet);
	}

	public static TmfExperiment makeTmfExperiment(File[] files) throws TmfTraceException, IOException {
		CtfTmfTrace[] ctf = new CtfTmfTrace[files.length];
		for (int i = 0; i < files.length; i++) {
			ctf[i] = new CtfTmfTrace();
			ctf[i].initTrace(null, files[i].getCanonicalPath(), ITmfEvent.class);
		}
		return makeTmfExperiment(ctf);
	}

	public TmfTimeRange getTimeRange() {
		return timeRange;
	}

}
