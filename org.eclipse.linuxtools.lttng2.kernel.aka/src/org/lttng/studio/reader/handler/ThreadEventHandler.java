package org.lttng.studio.reader.handler;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class ThreadEventHandler extends TraceEventHandlerBase {
	public long count;
	
	public ThreadEventHandler() {
		super();
		this.hooks.add(new TraceHook("sched_switch"));
	}
	
	@Override
	public void handleInit(TraceReader reader) {
		count = 0;
	} 

	@Override
	public void handleComplete(TraceReader reader) {
		System.out.println(count);
	}
	
	public void handle_all_event(TraceReader reader, EventDefinition event) {
		count++;
	}
}
