package org.lttng.studio.reader;

public class DummyTimeListener implements TimeListener {

	@Override
	public void begin(long min, long max) {
	}


	@Override
	public void progress(long time) {
	}

	@Override
	public void finished() {
	}


	@Override
	public void phase(int phase) {
	}


	@Override
	public boolean isCanceled() {
		return false;
	}

}
