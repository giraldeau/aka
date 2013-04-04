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

		int savedAntialias = gc.getAntialias();
		gc.setAntialias(SWT.ON);
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		if (entry.hasTimeEvents()) {
			long maxDuration = (timeProvider.getTimeSpace() == 0) ? Long.MAX_VALUE : 1 * (time1 - time0) / timeProvider.getTimeSpace();
			Iterator<ITimeEvent> iterator = entry.getTimeEventsIterator(time0, time1, maxDuration);
			while (iterator.hasNext()) {
				ITimeEvent event = iterator.next();
				if (event instanceof CriticalFlowLink) {
					CriticalFlowLink link = (CriticalFlowLink) event;
					int destIndex = ((CriticalFlowEntry) link.getDestEntry()).getPosition();
					int x0 = rect.x + (int) ((event.getTime() - time0) * pixelsPerNanoSec);
					Rectangle dst = getStatesRect(bounds, destIndex, nameSpace);
					int x1 = dst.x + (int) ((link.getTime() - time0) * pixelsPerNanoSec);
					int offset = rect.height >> 1;
					int y0 = rect.y + offset;
					int y1 = dst.y + offset;
					gc.drawLine(x0, y0, x1, y1);
					drawArrow(x1, x0, y1, y0, gc);
					//drawArrow2(x0, y0, x1, y1, gc);
				}
			}
		}
		gc.setAntialias(savedAntialias);
	}

	/*
	 * Source:
	 * http://stackoverflow.com/questions/3010803/draw-arrow-on-line-algorithm
	 */
	public static void drawArrow(int tipX, int tailX, int tipY, int tailY, GC gc) {
		int arrowLength = 12;
		int angle = 20;
		int dx = tipX - tailX;
		int dy = tipY - tailY;

		int posx = tailX + dx / 2;
		int posy = tailY + dy / 2;

		double theta = Math.atan2(dy, dx);

		double rad = Math.toRadians(angle);
		long x = Math.round(posx - arrowLength * Math.cos(theta + rad));
		long y = Math.round(posy - arrowLength * Math.sin(theta + rad));

		double phi2 = Math.toRadians(-angle);
		long x2 = Math.round(posx - arrowLength * Math.cos(theta + phi2));
		long y2 = Math.round(posy - arrowLength * Math.sin(theta + phi2));

		int[] arrow = new int[] { posx, posy, (int) x, (int) y, (int) x2,
				(int) y2 };
		gc.fillPolygon(arrow);
	}

	private static void drawArrow2(int x0, int y0, int x1, int y1, GC gc)
	{
		double cos = 0.866;
		double sin = 0.500;
		float len = 1;//Math.sqrt(((double)(x1 - x0)^2 + (y1 - y0)^2));
		
		float dx = (x1 - x0) / len;
		float dy = (y1 - y0) / len;
		int end1X = (int)(x1 + (dx * cos + dy * -sin));
		int end1Y = (int)(y1 + (dx * sin + dy * cos));
		int end2X = (int)(x1 + (dx * cos + dy * sin));
		int end2Y = (int)(y1 + (dx * -sin + dy * cos));
		gc.drawLine(x1, y1, end1X, end1Y);
		gc.drawLine(x1, y1, end2X, end2Y);
	}
	
}
