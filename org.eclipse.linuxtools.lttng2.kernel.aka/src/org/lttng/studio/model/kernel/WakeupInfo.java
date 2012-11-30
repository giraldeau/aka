package org.lttng.studio.model.kernel;


public class WakeupInfo {

	public enum Type { TIMER, SOCK, WAITPID };

	public long vec;
	public long sk;
	public long seq;
	public Task awakener;
	public Type type;

	@Override
	public String toString() {
		return "[wakeup " + type + "]";
	}

}
