package org.eclipse.linuxtools.lttng2.kernel.aka.views.criticalflow;

import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeEvent;

public class CriticalFlowLink extends TimeEvent {


	private ITimeGraphEntry destEntry;
	private long endTime;

	private CriticalFlowLink(ITimeGraphEntry entry, long startTime, long duration) {
		super(entry, startTime, duration);
	}

	public CriticalFlowLink(ITimeGraphEntry src, ITimeGraphEntry dst, long startTime, long endTime) {
		super(src, startTime, endTime - startTime);
		this.destEntry = dst;
		this.endTime = endTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public ITimeGraphEntry getDestEntry() {
		return destEntry;
	}

}
