package org.lttng.studio.reader.handler;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.lttng.studio.model.kernel.EventCounter;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class TraceEventHandlerCounter extends TraceEventHandlerBase {

	EventCounter counter;

	public TraceEventHandlerCounter(Integer priority) {
		super(priority);
		hooks.add(new TraceHook());
	}

	public TraceEventHandlerCounter() {
		this(0);
	}

	@Override
	public void handleInit(TraceReader reader) {
		counter = ModelRegistry.getInstance().getOrCreateModel(reader, EventCounter.class);
		counter.reset();
	}

	public void handle_all_event(TraceReader reader, EventDefinition event) {
		counter.increment();
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

	public long getCounter() {
		return counter.getCounter();
	}
}
