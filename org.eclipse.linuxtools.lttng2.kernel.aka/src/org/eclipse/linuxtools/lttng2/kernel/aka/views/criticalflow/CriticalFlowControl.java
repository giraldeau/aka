package org.eclipse.linuxtools.lttng2.kernel.aka.views.criticalflow;

import java.util.Iterator;

import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.widgets.ITimeDataProvider;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.widgets.TimeGraphColorScheme;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class CriticalFlowControl extends TimeGraphControl {

	public CriticalFlowControl(Composite parent, TimeGraphColorScheme colors) {
		super(parent, colors);
	}
	
	@Override
	public void drawItems(Rectangle bounds, ITimeDataProvider timeProvider,
            Item[] items, int topIndex, int nameSpace, GC gc) {
		super.drawItems(bounds, timeProvider, items, topIndex, nameSpace, gc);
		for (int i = topIndex; i < items.length; i++) {
            Item item = items[i];
            drawLinks(item, bounds, timeProvider, i, nameSpace, gc);
        }
	}

	protected void drawLinks(Item item, Rectangle bounds, ITimeDataProvider timeProvider, int i, int nameSpace, GC gc) {
		ITimeGraphEntry entry = item._trace;
        long time0 = timeProvider.getTime0();
        long time1 = timeProvider.getTime1();
        Rectangle rect = getStatesRect(bounds, i, nameSpace);
     // K pixels per second
        double pixelsPerNanoSec = (rect.width <= RIGHT_MARGIN) ? 0 : (double) (rect.width - RIGHT_MARGIN) / (time1 - time0);

        if (entry.hasTimeEvents()) {
            long maxDuration = (timeProvider.getTimeSpace() == 0) ? Long.MAX_VALUE : 1 * (time1 - time0) / timeProvider.getTimeSpace();
        	Iterator<ITimeEvent> iterator = entry.getTimeEventsIterator(time0, time1, maxDuration);
        	while (iterator.hasNext()) {
                ITimeEvent event = iterator.next();
                if (event instanceof CriticalFlowLink) {
                	CriticalFlowLink link = (CriticalFlowLink) event;
                	int destIndex = ((CriticalFlowEntry) link.getDestEntry()).getPosition();
                	
                    gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
                    
                    int x0 = rect.x + (int) ((event.getTime() - time0) * pixelsPerNanoSec);
                    
                    Rectangle dst = getStatesRect(bounds, destIndex, nameSpace);
                    int x1 = dst.x + (int) ((link.getTime() - time0) * pixelsPerNanoSec);
                    int offset = rect.height >> 1;
                    gc.drawLine(x0, rect.y + offset, x1, dst.y + offset);
                }
            }
        }

	}
	
}
