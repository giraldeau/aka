package org.lttng.studio.reader;

public interface TimeListener {

	public void begin(long min, long max);
	public void progress(long time);
	public void phase(int phase);
	public void finished();
	public boolean isCanceled();

}
