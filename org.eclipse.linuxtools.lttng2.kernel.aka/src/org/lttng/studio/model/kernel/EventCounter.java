package org.lttng.studio.model.kernel;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.lttng.studio.reader.TraceReader;

public class EventCounter implements ITraceModel {

	private long counter;

	public EventCounter() {
		setCounter(0);
	}

	public long getCounter() {
		return counter;
	}

	public void setCounter(long counter) {
		this.counter = counter;
	}

	public void increment() {
		counter++;
	}

	@Override
	public void reset() {
		counter = 0;
	}

	@Override
	public void init(TraceReader reader, CtfTmfTrace trace) {
		setCounter(0);
	}

}
