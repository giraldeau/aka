package org.lttng.studio.reader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.request.TmfDataRequest;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.reader.handler.ITraceEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerBase;
import org.lttng.studio.utils.CTFUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class TraceReader {

	class MinMax {
		public long min;
		public long max;
	}

	private final ModelRegistry registry;
	private final Map<Class<?>, ITraceEventHandler> handlers;
	private final ArrayList<TraceHook> catchAllBeforeHook;
	private final ArrayList<TraceHook> catchAllAfterHook;
	private final ArrayListMultimap<String, TraceHook> eventHookMap;
	private final ArrayListMultimap<String, TraceHook> eventHookCacheMap;
	private static Class<?>[] argTypes = new Class<?>[] { TraceReader.class, CtfTmfEvent.class };
	private final HashMap<CtfTmfTrace, Integer> traceCpus;
	private final ArrayListMultimap<Host, CtfTmfTrace> hostTraces;
	private final HashMap<String, Host> uuidStrToHost;
	private ITmfTrace mainTrace;
	private long now;
	private boolean cancel;
	private Exception exception;
	private TmfDataRequest request;
	private TmfTimeRange timeRange;

	public TraceReader() {
		handlers = new HashMap<Class<?>, ITraceEventHandler>();
		catchAllBeforeHook = new ArrayList<TraceHook>();
		catchAllAfterHook = new ArrayList<TraceHook>();
		registry = new ModelRegistry();
		eventHookMap = ArrayListMultimap.create();
		eventHookCacheMap = ArrayListMultimap.create();
		hostTraces = ArrayListMultimap.create();
		traceCpus = new HashMap<CtfTmfTrace, Integer>();
		uuidStrToHost = new HashMap<String, Host>();
	}

	public void registerHook(ITraceEventHandler handler, TraceHook hook) {
		String methodName;
		if (hook.isAllEvent()) {
			methodName = "handle_all_event_after";
		} else {
			methodName = "handle_" + hook.eventName;
		}
		boolean isHookOk = true;
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
			catchAllAfterHook.add(hook);
		} else {
			eventHookMap.put(hook.eventName, hook);
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
		if (mainTrace == null)
			throw new RuntimeException("Trace can't be null");

		mainTrace.seekEvent(Integer.MAX_VALUE);
		timeRange = mainTrace.getTimeRange();
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
					List<TraceHook> hookSet = getEventHookSet(evName);
					TraceReader.this.runHookSet(hookSet, ctf);
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
		mainTrace.sendRequest(request);
		request.waitForCompletion();

		// Re-throw any handler exception
		if (exception != null)
			throw exception;

		for(ITraceEventHandler handler: handlers.values()) {
			handler.handleComplete(this);
		}

		listener.finished();
	}

	private List<TraceHook> getEventHookSet(String evName) {
		List<TraceHook> list = eventHookCacheMap.get(evName);
		if (list.isEmpty()) {
			list.addAll(catchAllBeforeHook);
			list.addAll(eventHookMap.get(evName));
			list.addAll(catchAllAfterHook);
			Collections.sort(list);
		}
		return list;
	}

	public void runHookSet(List<TraceHook> hooks, CtfTmfEvent event) {
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

	public void updateTraceInfoCache() {
		traceCpus.clear();
		hostTraces.clear();
		uuidStrToHost.clear();
		List<CtfTmfTrace> list = CTFUtils.getCtfTraceList(this.mainTrace);
		for (CtfTmfTrace ctfTrace: list) {
			int num = CTFUtils.getNumCpuFromCtfTrace(ctfTrace);
			traceCpus.put(ctfTrace, num);
			String uuidStr = CTFUtils.getTraceClockUUIDStringRaw(ctfTrace.getCTFTrace());
			Host host = uuidStrToHost.get(uuidStr);
			if (host == null) {
				host = CTFUtils.hostFromTrace(ctfTrace);
				uuidStrToHost.put(uuidStr, host);
			}
			hostTraces.put(host, ctfTrace);
		}
	}

	public void setTrace(ITmfTrace trace) {
		this.mainTrace = trace;
		updateTraceInfoCache();
	}

	public Host getHost(CtfTmfTrace ctfTrace) {
		return uuidStrToHost.get(ctfTrace.getCTFTrace().getClock().getProperty(CTFUtils.UUID_FIELD));
	}

	public Multimap<Host, CtfTmfTrace> getTraceHostMap() {
		return hostTraces;
	}

	public void setMainTrace(File file) throws TmfTraceException, IOException {
		CtfTmfTrace ctfTrace = CTFUtils.makeCtfTrace(file);
		setTrace(ctfTrace);
	}

	public ITmfTrace getMainTrace() {
		return this.mainTrace;
	}

	public int getNumCpus(ITmfTrace trace) {
		return traceCpus.get(trace);
	}

	public void clearHandlers() {
		this.handlers.clear();
		this.eventHookCacheMap.clear();
		this.eventHookMap.clear();
		this.catchAllAfterHook.clear();
		this.catchAllBeforeHook.clear();
	}

	public ModelRegistry getRegistry() {
		return registry;
	}

	/*
	 * FIXME: lookup num CPU must be done per trace
	 */
	public int getNumCpus() {
		return 0;
	}

	public long getCurrentTime() {
		return now;
	}

	public List<CtfTmfTrace> getTraces() {
		return CTFUtils.getCtfTraceList(mainTrace);
	}

}
