package org.lttng.studio.model.kernel;

import java.util.EnumMap;
import java.util.Map;

import org.lttng.studio.model.kernel.Task.execution_mode;
import org.lttng.studio.model.kernel.Task.process_status;

public abstract class StateInfo {

	public enum Field {
		PREV_PID, NEXT_PID, PARENT_PID, CHILD_PID,
		PID, TGID, CHILD_TGID, FILENAME, IP, FD,
		STATE, CPU_ID, SRC_ADDR, SRC_PORT, DST_ADDR, DST_PORT,
		SOCKET, IS_CLIENT
	}

	private Task task;
	private execution_mode mode;
	private process_status status;
	private long start;
	private long end;
	private Map<Field, Object> fieldInfo;

	public StateInfo() {
	}

	public void setStartTime(long start) {
		this.start = start;
	}

	public long getStartTime() {
		return start;
	}

	public void setEndTime(long end) {
		this.end = end;
	}

	public long getEndTime() {
		return end;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public Task getTask() {
		return task;
	}
	public long getDuration() {
		return end - start;
	}

	public void setField(Field key, Object value) {
		if (fieldInfo == null)
			fieldInfo = new EnumMap<Field, Object>(Field.class);
		fieldInfo.put(key, value);
	}

	public Object getField(Field key) {
		if (fieldInfo == null || !fieldInfo.containsKey(key))
			return null;
		return fieldInfo.get(key);
	}

	public execution_mode getMode() {
		return mode;
	}

	public void setMode(execution_mode mode) {
		this.mode = mode;
	}

	public process_status getStatus() {
		return status;
	}

	public void setStatus(process_status status) {
		this.status = status;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("[")
			.append(task.toString()).append(",")
			.append(fieldInfo).append("]");
		return str.toString();
	}
}
