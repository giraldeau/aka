package org.lttng.studio.reader;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

public class TimeLoadingListener implements TimeListener {

	IProgressMonitor monitor;
	double scale;
	long delta;
	long prev;
	long interval = 1000000; // 1ms
	long min;
	long max;
	int prevStep = 0;
	int progressMax = 1000;
	int progressPhase = 0;
	int numPhases = 1;
	int phase = 0;
	private String name;

	public TimeLoadingListener(String name, int numPhases, IProgressMonitor listener) {
		setLoadingListener(listener);
		setName(name);
		if (numPhases < 1)
			numPhases = 1;
		this.numPhases = numPhases;
		this.progressPhase = progressMax / numPhases;
	}

	@Override
	public void begin(long min, long max) {
		this.min = min;
		this.max = max;
		this.scale = 1.0f / (numPhases * ((double) max - min)) * progressMax;
		this.prev = min;
		this.delta = 0;
		monitor.beginTask(getName(), progressMax);
	}

	@Override
	public void progress(long time) {
		delta += time - prev;
		if (delta > interval) {
			delta = 0;
			int step = (int) ((time - min) * scale);
			step += phase * progressPhase;
			if (step != prevStep) {
				monitor.worked(step);
				prevStep = step;
			}
		}
	}

	@Override
	public void finished() {
		monitor.done();
	}

	private void setLoadingListener(IProgressMonitor monitor) {
		if (monitor == null) {
			this.monitor = new NullProgressMonitor();
		} else {
			this.monitor = monitor;
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name == null) {
			this.name = "Default";
		} else {
			this.name = name;
		}
	}

	@Override
	public void phase(int phase) {
		this.phase = phase;
	}

	@Override
	public boolean isCanceled() {
		return this.monitor.isCanceled();
	}

}
