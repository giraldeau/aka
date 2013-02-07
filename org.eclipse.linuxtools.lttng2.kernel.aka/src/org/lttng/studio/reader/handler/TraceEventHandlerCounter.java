package org.lttng.studio.reader.handler;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.lttng.studio.model.kernel.EventCounter;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class TraceEventHandlerCounter extends TraceEventHandlerBase {

	EventCounter counter;

	public TraceEventHandlerCounter() {
		super();
		hooks.add(new TraceHook());
	}

	@Override
	public void handleInit(TraceReader reader) {
		counter = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, EventCounter.class);
		counter.reset();
	}

	public void handle_all_event(TraceReader reader, CtfTmfEvent event) {
		counter.increment();
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}
}
