package org.lttng.studio.reader;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.linuxtools.ctf.core.trace.CTFTraceReader;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;

public class AnalyzerThread extends Thread {
	private final ArrayList<CtfTmfTrace> traces;
	private TimeListener listener;
	private final Collection<AnalysisPhase> phases;
	private final TraceReader reader;

	public AnalyzerThread() {
		super();
		setListener(null);
		reader = new TraceReader();
		traces = new ArrayList<CtfTmfTrace>();
		phases = new ArrayList<AnalysisPhase>();
	}

	@Override
	public void run() {
		int curr = 0;
		for (CtfTmfTrace t: traces) {
			reader.addReader(new CTFTraceReader(t.getCTFTrace()));
		}
		for (AnalysisPhase phase: phases) {
			listener.phase(curr);
			processOnePhase(phase);
			curr++;
		}
	}

	private void processOnePhase(AnalysisPhase phase) {
		reader.clearHandlers();
		reader.registerAll(phase.getHandlers());
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

	public void addPhase(AnalysisPhase phase) {
		if (phase == null)
			return;
		this.phases.add(phase);
	}

	public int getNumPhases() {
		if (this.phases != null)
			return this.phases.size();
		return 0;
	}

}
