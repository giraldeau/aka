package org.lttng.studio.model.kernel;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;

public class InterruptContext {

	public enum Context {
		SOFTIRQ, IRQ, HRTIMER, NONE
	}
	
	private final CtfTmfEvent event;
	private final Context context;
	
	public InterruptContext(CtfTmfEvent event, Context ctx) {
		this.event = event;
		this.context = ctx;
	}
	
	public CtfTmfEvent getEvent() {
		return event;
	}
	public Context getContext() {
		return context;
	}
	
}
