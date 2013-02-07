package org.lttng.studio.reader.handler;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class TraceEventHandlerInvariant extends TraceEventHandlerBase {

	private long prev = 0;

	public TraceEventHandlerInvariant() {
		super();
		hooks.add(new TraceHook());
	}

	@Override
	public void handleInit(TraceReader reader) {
		prev = 0;
	}

	public void handle_all_event(TraceReader reader, CtfTmfEvent event) {
		long ts = event.getTimestamp().getValue();
		//System.out.println(prev + " " + ts);
		if (prev > ts) {
			reader.cancel(new RuntimeException("Error: prev timestamps is greater than current timestamps"));
		}
		prev = ts;
	}

	@Override
	public void handleComplete(TraceReader reader) {

	}

}
