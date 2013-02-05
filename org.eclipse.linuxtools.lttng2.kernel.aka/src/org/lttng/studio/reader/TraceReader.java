package org.lttng.studio.reader;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
import org.eclipse.linuxtools.ctf.core.trace.CTFTrace;
import org.eclipse.linuxtools.ctf.core.trace.CTFTraceReader;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.reader.handler.ITraceEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerBase;

public class TraceReader {

	class MinMax {
		public long min;
		public long max;
	}

	private final ModelRegistry registry;
	private final List<CTFTraceReader> readers;
	private final Map<Class<?>, ITraceEventHandler> handlers;
	private final Map<String, TreeSet<TraceHook>> eventHookMap;
	private final TreeSet<TraceHook> catchAllHook;
	private static Class<?>[] argTypes = new Class<?>[] { TraceReader.class, EventDefinition.class };
	private final TimeKeeper timeKeeper;
	private boolean cancel;
	private int nbCpus;
	private Exception exception;
	private CTFTraceReader currentReader;

	public TraceReader() {
		handlers = new HashMap<Class<?>, ITraceEventHandler>();
		eventHookMap = new HashMap<String, TreeSet<TraceHook>>();
		catchAllHook = new TreeSet<TraceHook>();
		readers = new ArrayList<CTFTraceReader>();
		timeKeeper = TimeKeeper.getInstance();
		registry = new ModelRegistry();
	}

	public void loadTrace() throws CTFReaderException {
		checkNumStreams();
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

	public void process(TimeListener listener) throws Exception {
		loadTrace();
		EventDefinition event;
		String eventName;
		cancel = false;

		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		for (CTFTraceReader reader: readers) {
			// FIXME: bug that returns null event:
			// reader.goToLastEvent()
			// reader.seek(reader.getEndTime())
			// reader.getCurrentEventDef() ---> returns null, shouldn't
			// FIXME: other bug: start1 != start2
			// FIXME: other bug: second call to goToLastEvent() returns null event
			reader.seek(0);
			//long start1 = reader.getStartTime() + reader.getTrace().getOffset();
			long start2 = reader.getCurrentEventDef().getTimestamp() + reader.getTrace().getOffset();
			//System.out.println(String.format("start t1=%d t2=%d diff=%d", start1, start2, start1 - start2));

			reader.goToLastEvent();
			long end1 = reader.getEndTime();
			//long end2 = reader.getCurrentEventDef().getTimestamp() + reader.getTrace().getOffset();
			//System.out.println(String.format("end t1=%d t2=%d diff=%d", end1, end2, end1 - end2));

			min = Math.min(min, start2);
			max = Math.max(max, end1);
			reader.seek(0);
		}
		listener.begin(min, max);

		for(ITraceEventHandler handler: handlers.values()) {
			if (cancel == true)
				break;
			handler.handleInit(this);
		}
		// Re-throw any handler exception
		if (exception != null)
			throw exception;

		PriorityQueue<CTFTraceReader> prio = new PriorityQueue<CTFTraceReader>(readers.size(), new CTFTraceReaderComparator());
		prio.addAll(readers);
		//while((event=getReader().getCurrentEventDef()) != null && cancel == false) {
		while((setCurrentReader(prio.poll())) != null) {
			if (listener.isCanceled()) {
				break;
			}
			event = getCurrentCtfReader().getCurrentEventDef();
			if (event == null)
				continue;
			if (cancel == true || exception != null)
				break;
			long now = event.getTimestamp()+getCurrentCtfReader().getTrace().getOffset();
			timeKeeper.setCurrentTime(now);
			listener.progress(now);
			eventName = event.getDeclaration().getName();
			TreeSet<TraceHook> treeSet = eventHookMap.get(eventName);
			if (treeSet != null)
				runHookSet(treeSet, event);
			runHookSet(catchAllHook, event);
			getCurrentCtfReader().advance();
			prio.add(getCurrentCtfReader());
		}

		// Re-throw any handler exception
		if (exception != null)
			throw exception;

		for(ITraceEventHandler handler: handlers.values()) {
			handler.handleComplete(this);
		}

		listener.finished();
	}

	public void runHookSet(TreeSet<TraceHook> hooks, EventDefinition event) {
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

	public void addReader(CTFTraceReader reader) {
		readers.add(reader);
	}

	public void checkNumStreams() {
		if (readers.isEmpty())
			return;
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (CTFTraceReader reader: readers) {
			int num = getNumStreams(reader);
			min = Math.min(num, min);
			max = Math.max(num, max);
		}
		if (min != max) {
			throw new RuntimeException("All traces must have the same number of streams");
		}
		setNbCpus(max);
	}

	public static int getNumStreams(CTFTraceReader reader) {
		Field field;
		try {
			field = reader.getClass().getDeclaredField("streamInputReaders");
			field.setAccessible(true);
			Vector v = (Vector) field.get(reader);
			return v.size();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error trying to retreive the number of CPUs of the trace");
		}
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
	}

	public void addTrace(File file) throws CTFReaderException {
		CTFTraceReader reader = new CTFTraceReader(new CTFTrace(file));
		addReader(reader);
	}

	public List<CTFTraceReader> getCTFTraceReaders() {
		return readers;
	}

	public CTFTraceReader getCurrentCtfReader() {
		return currentReader;
	}

	public CTFTraceReader setCurrentReader(CTFTraceReader currentReader) {
		this.currentReader = currentReader;
		return currentReader;
	}

	public static long clockTime(EventDefinition event) {
		return (Long) event.getStreamInputReader()
							.getParent()
							.getTrace()
							.getClock()
							.getProperty("offset") + event.getTimestamp();
	}

	public ModelRegistry getRegistry() {
		return registry;
	}
}
