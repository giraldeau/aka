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
	int prevStep = 0;
	int progressMax = 1000;
	private String name;

	public TimeLoadingListener(String name, IProgressMonitor listener) {
		setLoadingListener(listener);
		setName(name);
	}

	@Override
	public void begin(long min, long max) {
		this.min = min;
		this.scale = 1.0f / ((double) max - min) * progressMax;
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

}
