package org.eclipse.linuxtools.lttng2.kernel.aka.views.criticalflow;

import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.widgets.TimeGraphColorScheme;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.swt.widgets.Composite;

public class CriticalFlowViewer extends TimeGraphViewer {

	public CriticalFlowViewer(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	protected TimeGraphControl createTimeGraphControl(Composite parent, TimeGraphColorScheme colors) {
		return new CriticalFlowControl(parent, colors);
	}
	
}
