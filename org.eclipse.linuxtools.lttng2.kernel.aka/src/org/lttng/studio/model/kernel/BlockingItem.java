package org.lttng.studio.model.kernel;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;

/*
 * Represents when a task is blocked waiting
 */
public class BlockingItem {

	private EventDefinition syscall;
	private WakeupInfo wakeup;
	private Task task;

	public BlockingItem() {
	}

	public EventDefinition getSyscall() {
		return syscall;
	}

	public void setSyscall(EventDefinition syscall) {
		this.syscall = syscall;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("[");
		if (syscall != null) {
			str.append(syscall.getDeclaration().getName());
		} else {
			str.append("unknown");
		}
		str.append(",");
		return str.toString();
	}

	public void setWakeupInfo(WakeupInfo info) {
		this.wakeup = info;
	}

	public WakeupInfo getWakeupInfo() {
		return this.wakeup;
	}

	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}
}
