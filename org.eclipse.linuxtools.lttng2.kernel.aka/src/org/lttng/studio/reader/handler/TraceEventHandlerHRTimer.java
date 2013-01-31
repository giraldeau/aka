package org.lttng.studio.reader.handler;

import java.util.HashMap;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDefinition;
import org.lttng.studio.model.kernel.HRTimer;
import org.lttng.studio.model.kernel.HRTimer.HRTimerState;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class TraceEventHandlerHRTimer extends TraceEventHandlerBase {

	SystemModel system;

	/*
	 * hrtimer_cancel:
     * hrtimer_init:
     * hrtimer_start:
	 */

	public TraceEventHandlerHRTimer() {
		super();
		hooks.add(new TraceHook("hrtimer_cancel"));
		hooks.add(new TraceHook("hrtimer_init"));
		hooks.add(new TraceHook("hrtimer_start"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
	}

	private HRTimer getOrCreateHRTimer(long id) {
		HRTimer timer = system.getHRTimers().get(id);
		if (timer == null) {
			timer = new HRTimer(id, HRTimerState.INIT);
			system.getHRTimers().put(id, timer);	
		}
		return timer;
	}

	private void handleHRTimerEventGeneric(EventDefinition event, HRTimerState state) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		IntegerDefinition hrtimerIdDef = (IntegerDefinition) def.get("_hrtimer");
		HRTimer timer = getOrCreateHRTimer(hrtimerIdDef.getValue());
		timer.setState(state);
	}
	
	public void handle_hrtimer_init(TraceReader reader, EventDefinition event) {
		handleHRTimerEventGeneric(event, HRTimerState.INIT);
	}

	public void handle_hrtimer_start(TraceReader reader, EventDefinition event) {
		handleHRTimerEventGeneric(event, HRTimerState.START);
	}

	public void handle_hrtimer_cancel(TraceReader reader, EventDefinition event) {
		handleHRTimerEventGeneric(event, HRTimerState.CANCEL);
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

}
