package org.lttng.studio.model.graph;

import org.lttng.studio.model.kernel.Task;

public class ExecVertex {

	private final long timestamp;
	private final Task task;

	public ExecVertex(Task task, long timestamp) {
		this.task = task;
		this.timestamp = timestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Task getTask() {
		return task;
	}

}
