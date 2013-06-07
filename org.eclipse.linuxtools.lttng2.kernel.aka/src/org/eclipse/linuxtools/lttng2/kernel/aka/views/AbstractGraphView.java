package org.eclipse.linuxtools.lttng2.kernel.aka.views;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.jface.action.Action;
import org.eclipse.linuxtools.lttng2.kernel.aka.JobListener;
import org.eclipse.linuxtools.lttng2.kernel.aka.JobManager;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.zest.core.viewers.AbstractZoomableViewer;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IZoomableWorkbenchPart;
import org.eclipse.zest.core.viewers.ZoomContributionViewItem;
import org.eclipse.zest.layouts.algorithms.SugiyamaLayoutAlgorithm;
import org.lttng.studio.reader.AnalyzerThread;

public class AbstractGraphView extends TmfView implements
	IZoomableWorkbenchPart, JobListener {

    protected GraphViewer graphViewer;
    protected Composite content;
    private Rectangle bounds;
    private ITmfTrace fTrace;

    public AbstractGraphView(String viewName) {
	super(viewName);
    }

    @Override
    public AbstractZoomableViewer getZoomableViewer() {
	return graphViewer;
    }

    @Override
    public void createPartControl(Composite parent) {
	content = new Composite(parent, SWT.NONE);
	content.setLayout(new FillLayout());
	graphViewer = new GraphViewer(content, SWT.NONE);
	SugiyamaLayoutAlgorithm algorithm = new SugiyamaLayoutAlgorithm(
		SugiyamaLayoutAlgorithm.HORIZONTAL);
	graphViewer.setLayoutAlgorithm(algorithm);
	// Zoom
	IActionBars bars = getViewSite().getActionBars();
	ZoomContributionViewItem toolbar = new ZoomContributionViewItem(this);
	bars.getToolBarManager().add(toolbar);

	// receive signal about processed trace
	JobManager.getInstance().addListener(this);

	// FIXME: this is horrible, should find why does the layout can't expand
	// outside the scroll area to avoid overlapping
	Action action1 = new Action() {
	    int cnt = 1;

	    @Override
	    public void run() {
		cnt++;
		if (bounds == null)
		    bounds = graphViewer.getGraphControl().getClientArea();
		bounds.width = bounds.width * cnt;
		resetBounds();
	    }
	};
	action1.setText("+W");
	bars.getToolBarManager().add(action1);

	Action action2 = new Action() {
	    int cnt = 1;

	    @Override
	    public void run() {
		cnt++;
		if (bounds == null)
		    bounds = graphViewer.getGraphControl().getClientArea();
		bounds.height = bounds.height * cnt;
		resetBounds();
	    }
	};
	action2.setText("+H");
	bars.getToolBarManager().add(action2);
    }

    protected void resetBounds() {
	SugiyamaLayoutAlgorithm algorithm = new SugiyamaLayoutAlgorithm(
		SugiyamaLayoutAlgorithm.HORIZONTAL, new Dimension(bounds.width,
			bounds.height));
	graphViewer.setLayoutAlgorithm(algorithm, true);
	System.out.println(bounds);
    }

    @TmfSignalHandler
    public void traceSelected(final TmfTraceSelectedSignal signal) {
	if (signal.getTrace() == fTrace)
	    return;
	fTrace = signal.getTrace();
	JobManager.getInstance().launch(fTrace);
    }

    @Override
    public void setFocus() {
	graphViewer.getControl().setFocus();
    }

    @Override
    public void ready(AnalyzerThread thread) {
    }

}
