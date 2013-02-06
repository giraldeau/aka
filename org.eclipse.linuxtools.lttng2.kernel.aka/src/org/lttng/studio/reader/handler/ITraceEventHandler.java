package org.lttng.studio.reader.handler;

import java.util.Set;

import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public interface ITraceEventHandler extends Comparable<ITraceEventHandler> {

	public Set<TraceHook> getHooks();

	public void handleInit(TraceReader reader);

	public void handleComplete(TraceReader reader);

	public Integer getPriority();
}
