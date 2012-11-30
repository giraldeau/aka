package org.lttng.studio.reader;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.linuxtools.ctf.core.trace.CTFTraceReader;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.lttng.studio.reader.handler.ITraceEventHandler;

public class AnalyzerThread extends Thread {
	private final ArrayList<CtfTmfTrace> traces;
	private TimeListener listener;
	private final Collection<ITraceEventHandler> handlers;
	private final TraceReader reader;

	public AnalyzerThread() {
		super();
		reader = new TraceReader();
		traces = new ArrayList<CtfTmfTrace>();
		handlers = new ArrayList<ITraceEventHandler>();
	}

	@Override
	public void run() {
		for (CtfTmfTrace t: traces) {
			reader.addReader(new CTFTraceReader(t.getCTFTrace()));
		}
		reader.registerAll(handlers);
		try {
			reader.process(listener);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void cancel() {
		if (reader != null)
			reader.cancel();
	}

	public TimeListener getListener() {
		return listener;
	}

	public void setListener(TimeListener listener) {
		this.listener = listener;
		if (this.listener == null)
			this.listener = new DummyTimeListener();
	}

	public void addTrace(CtfTmfTrace info) {
		traces.add(info);
	}

	public void addAllTraces(Collection<CtfTmfTrace> info) {
		if (info == null)
			return;
		traces.addAll(info);
	}

	public Collection<CtfTmfTrace> getTraces() {
		return traces;
	}

	public TraceReader getReader() {
		return reader;
	}

	public void addAllHandlers(Collection<ITraceEventHandler> handlers) {
		if (handlers == null)
			return;
		this.handlers.addAll(handlers);
	}

}
