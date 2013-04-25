package org.lttng.studio.model.kernel;

import java.util.HashMap;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class Task {

	public enum thread_type_enum {
		USER_THREAD(0),
		KERNEL_THREAD(1);
		private final int val;

		private thread_type_enum(int val) {
			this.val = val;
		}
		public int value() { return val; }
	}

	public enum execution_mode_enum {
		USER_MODE(0),
		SYSCALL(1),
		TRAP(2),
		IRQ(3),
		SOFTIRQ(4),
		MODE_UNKNOWN(5);
		private final int val;

		private execution_mode_enum(int val) {
			this.val = val;
		}
		public int value() { return val; }
	}

	public enum execution_submode_enum {
		NONE(0),
		UNKNOWN(1);
		private final int val;

		private execution_submode_enum(int val) {
			this.val = val;
		}
		public int value() { return val; }
	}

	/*
	from lttng-statedump-impl.c:
	enum lttng_process_status {
        LTTNG_UNNAMED = 0,
        LTTNG_WAIT_FORK = 1,
        LTTNG_WAIT_CPU = 2,
        LTTNG_EXIT = 3,
        LTTNG_ZOMBIE = 4,
        LTTNG_WAIT = 5,
        LTTNG_RUN = 6,
        LTTNG_DEAD = 7,
	};

	from include/linux/sched.h
	#define TASK_RUNNING            0
	#define TASK_INTERRUPTIBLE      1
	#define TASK_UNINTERRUPTIBLE    2
	#define __TASK_STOPPED          4
	#define __TASK_TRACED           8
	// in tsk->exit_state
	#define EXIT_ZOMBIE             16
	#define EXIT_DEAD               32
	// in tsk->state again
	#define TASK_DEAD               64
	#define TASK_WAKEKILL           128
	#define TASK_WAKING             256
	#define TASK_STATE_MAX          512
	 */

	public enum process_status_enum {
		UNNAMED(0),
		WAIT_FORK(1),
		WAIT_CPU(2),
		EXIT(3),
		ZOMBIE(4),
		WAIT_BLOCKED(5),
		RUN(6),
		DEAD(7);
		private final int val;

		private process_status_enum(int val) {
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
	private process_status_enum process_status;
	private process_status_enum process_status_prev;
	private execution_mode_enum execution_mode;
	private execution_submode_enum execution_submode;
	private thread_type_enum thread_type;

	public Task() {
		this(0);
		this.process_status = process_status_enum.UNNAMED;
		this.process_status_prev = process_status_enum.UNNAMED;
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

	/**
	 * Returns the current process status
	 * @return
	 */
	public process_status_enum getProcessStatus() {
		return process_status;
	}

	/**
	 * Returns the previous state of t
	 * @return
	 */
	public process_status_enum getProcessStatusPrev() {
		return process_status_prev;
	}

	public void setProcessStatus(process_status_enum process_status) {
		this.process_status_prev = this.process_status;
		this.process_status = process_status;
	}

	/*
	 * I hate Java: can't just assign the enum from the int value
	 */
	public void setProcessStatus(long status) {
		for (process_status_enum e: process_status_enum.values()) {
			if (e.value() == status) {
				process_status = e;
				break;
			}
		}
	}

	public execution_mode_enum getExecutionMode() {
		return execution_mode;
	}

	public void setExecutionMode(long mode) {
		for (execution_mode_enum e: execution_mode_enum.values()) {
			if (e.value() == mode) {
				execution_mode = e;
				break;
			}
		}
	}

	public void setExecutionMode(execution_mode_enum execution_mode) {
		this.execution_mode = execution_mode;
	}

	public execution_submode_enum getExecutionSubmode() {
		return execution_submode;
	}

	public void setExecutionSubmode(execution_submode_enum execution_submode) {
		this.execution_submode = execution_submode;
	}

	public void setExecutionSubmode(long submode) {
		for (execution_submode_enum e: execution_submode_enum.values()) {
			if (e.value() == submode) {
				execution_submode = e;
				break;
			}
		}
	}

	public thread_type_enum getThreadType() {
		return thread_type;
	}

	public void setThreadType(thread_type_enum thread_type) {
		this.thread_type = thread_type;
	}

	public void setThreadType(long type) {
		for (thread_type_enum e: thread_type_enum.values()) {
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
