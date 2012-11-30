package org.lttng.studio.reader.handler;

import java.util.HashSet;
import java.util.Set;

import org.lttng.studio.model.kernel.Notifier;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class TraceEventHandlerBase extends Notifier implements ITraceEventHandler {

	private static int autoPriority = 0;
	protected Set<TraceHook> hooks;
	private final Integer priority;

	public TraceEventHandlerBase(Integer priority) {
		super();
		this.hooks = new HashSet<TraceHook>();
		this.priority = priority;
	}

	public TraceEventHandlerBase() {
		this(autoPriority++);
	}

	@Override
	public Set<TraceHook> getHooks() {
		return hooks;
	}

	public void setHooks(Set<TraceHook> hooks) {
		this.hooks = hooks;
	}

	@Override
	public void handleInit(TraceReader reader) {

	}

	@Override
	public void handleComplete(TraceReader reader) {

	}

	@Override
	public Integer getPriority() {
		return priority;
	}

	@Override
	public int compareTo(ITraceEventHandler other) {
		return priority.compareTo(other.getPriority());
	}
}
