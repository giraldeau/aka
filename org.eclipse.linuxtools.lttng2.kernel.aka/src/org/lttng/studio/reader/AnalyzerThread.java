package org.lttng.studio.reader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

public class AnalyzerThread extends Thread {

	private TimeListener listener;
	private final SortedSet<AnalysisPhase> phases;
	private final TraceReader reader;

	public AnalyzerThread() {
		super();
		setListener(null);
		reader = new TraceReader();
		phases = new TreeSet<AnalysisPhase>();
	}

	@Override
	public void run() {
		int curr = 0;
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

	public TraceReader getReader() {
		return reader;
	}

	public void addPhase(AnalysisPhase phase) {
		if (phase == null)
			return;
		this.phases.add(phase);
	}

	public void addAllPhases(Collection<AnalysisPhase> phases) {
		if (phases == null)
			return;
		this.phases.addAll(phases);
	}

	public int getNumPhases() {
		if (this.phases != null)
			return this.phases.size();
		return 0;
	}

	public void setTrace(ITmfTrace trace) {
		reader.setTrace(trace);
	}

	public void setTrace(File trace) throws TmfTraceException, IOException {
		reader.setMainTrace(trace);
	}

}
