package org.lttng.studio.model.kernel;

import java.util.HashMap;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class Task {

	public enum thread_type {
		USER_THREAD(0),
		KERNEL_THREAD(1);
		private final int val;

		private thread_type(int val) {
			this.val = val;
		}
		public int value() { return val; }
	}

	public enum execution_mode {
		USER_MODE(0),
		SYSCALL(1),
		TRAP(2),
		IRQ(3),
		SOFTIRQ(4),
		MODE_UNKNOWN(5);
		private final int val;

		private execution_mode(int val) {
			this.val = val;
		}
		public int value() { return val; }
	}

	public enum execution_submode {
		NONE(0),
		UNKNOWN(1);
		private final int val;

		private execution_submode(int val) {
			this.val = val;
		}
		public int value() { return val; }
	}

	public enum process_status {
		UNNAMED(0),
		WAIT_FORK(1),
		WAIT_CPU(2),
		EXIT(3),
		ZOMBIE(4),
		WAIT(5),
		RUN(6),
		DEAD(7);
		private final int val;

		private process_status(int val) {
			this.val = val;
		}
		public int value() { return val; }
	}

	private long pid;
	private long ppid;
	private long tid;
	private long start;
	private long end;
	private String name;
	private HashMap<Long, String> fdMap;
	private process_status process_status;
	private execution_mode execution_mode;
	private execution_submode execution_submode;
	private thread_type thread_type;

	public Task() {
		this(0);
		setFdMap(new HashMap<Long, String>());
	}

	public Task(long tid) {
		setTid(tid);
	}

	public long getPid() {
		return pid;
	}

	public void setPid(long pid) {
		this.pid = pid;
	}

	public long getTid() {
		return tid;
	}

	public void setTid(long tid) {
		this.tid = tid;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	public HashMap<Long, String> getFdMap() {
		return fdMap;
	}

	public void setFdMap(HashMap<Long, String> fdMap) {
		this.fdMap = fdMap;
	}

	public void addFileDescriptor(long fd, String filename) {
		fdMap.put(fd, filename);
	}

	public void removeFileDescriptor(long fd) {
		fdMap.remove(fd);
	}

	public process_status getProcessStatus() {
		return process_status;
	}

	public void setProcessStatus(process_status process_status) {
		this.process_status = process_status;
	}

	/*
	 * I hate Java: can't just assign the enum from the int value
	 */
	public void setProcessStatus(long status) {
		for (process_status e: process_status.values()) {
			if (e.value() == status) {
				process_status = e;
				break;
			}
		}
	}

	public execution_mode getExecutionMode() {
		return execution_mode;
	}

	public void setExecutionMode(long mode) {
		for (execution_mode e: execution_mode.values()) {
			if (e.value() == mode) {
				execution_mode = e;
				break;
			}
		}
	}

	public void setExecutionMode(execution_mode execution_mode) {
		this.execution_mode = execution_mode;
	}

	public execution_submode getExecutionSubmode() {
		return execution_submode;
	}

	public void setExecutionSubmode(execution_submode execution_submode) {
		this.execution_submode = execution_submode;
	}

	public void setExecutionSubmode(long submode) {
		for (execution_submode e: execution_submode.values()) {
			if (e.value() == submode) {
				execution_submode = e;
				break;
			}
		}
	}

	public thread_type getThreadType() {
		return thread_type;
	}

	public void setThreadType(thread_type thread_type) {
		this.thread_type = thread_type;
	}

	public void setThreadType(long type) {
		for (thread_type e: thread_type.values()) {
			if (e.value() == type) {
				thread_type = e;
				break;
			}
		}
	}

	public long getPpid() {
		return ppid;
	}

	public void setPpid(long ppid) {
		this.ppid = ppid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return String.format("[%d,%s]", tid, name);
	}

	/*
	 * Equals with TID and start time, because TID may wrap.
	 * In the case of a distributed system, this key may not
	 * be unique, but assume it's handled at another level.
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (!(other instanceof Task))
			return false;
		Task o = (Task) other;
		if (o.getTid() == this.getTid() && o.getStart() == this.getStart())
			return true;
		return false;
	}

	@Override
	public int hashCode() {
		HashFunction hf = Hashing.goodFastHash(32);
		Hasher hasher = hf.newHasher();
		HashCode hc = hasher
				.putLong(getTid())
				.putLong(getStart())
				.hash();
		return hc.asInt();
	}

	public boolean isThreadGroupLeader() {
		return pid == tid;
	}

}
