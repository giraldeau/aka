package org.eclipse.linuxtools.lttng2.kernel.aka.views;


import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.IFigure;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.signal.TmfExperimentSelectedSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.zest.core.viewers.AbstractZoomableViewer;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IConnectionStyleProvider;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;
import org.eclipse.zest.core.viewers.IZoomableWorkbenchPart;
import org.eclipse.zest.core.viewers.ZoomContributionViewItem;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.SugiyamaLayoutAlgorithm;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * Basic graph view
 */

public class TaskHierarchyView extends TmfView implements IZoomableWorkbenchPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.eclipse.linuxtools.lttng2.kernel.aka.views.TaskHierarchyView";

	private TmfExperiment<ITmfEvent> fSelectedExperiment;

	private Label label;

	private GraphViewer graphViewer;

	private Composite content;

	private DirectedGraph<String,DefaultEdge> stringGraph;

	public class ZestJGraphtNodeProvider extends ArrayContentProvider implements IGraphEntityContentProvider {
		  @Override
		  public Object[] getConnectedTo(Object entity) {
		    if (entity instanceof String) {
		      String node = (String) entity;
		      List<String> neighbors = Graphs.successorListOf(stringGraph, node);
		      return neighbors.toArray();
		    }
		    throw new RuntimeException("Type not supported");
		  }
	}

	public class ZestJgraphTLabelProvider extends LabelProvider implements IConnectionStyleProvider {
		  @Override
		  public String getText(Object element) {
		    if (element instanceof String) {
		      String node = (String) element;
		      return node;
		    }
		    // Not called with the IGraphEntityContentProvider
		    if (element instanceof DefaultEdge) {
		      DefaultEdge edge = (DefaultEdge) element;
		      return edge.toString();
		    }

		    if (element instanceof EntityConnectionData) {
		      EntityConnectionData test = (EntityConnectionData) element;
		      return "";
		    }
		    throw new RuntimeException("Wrong type: "
		        + element.getClass().toString());
		  }

		@Override
		public int getConnectionStyle(Object rel) {
			return ZestStyles.CONNECTIONS_DIRECTED;
		}

		@Override
		public Color getColor(Object rel) {
			return null;
		}

		@Override
		public Color getHighlightColor(Object rel) {
			return null;
		}

		@Override
		public int getLineWidth(Object rel) {
			return -1;
		}

		@Override
		public IFigure getTooltip(Object entity) {
			return null;
		}

		@Override
		public ConnectionRouter getRouter(Object rel) {
			return null;
		}
		}

	/**
	 * The constructor.
	 */
	public TaskHierarchyView() {
		super("Task Hierarchy View");
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		content = new Composite(parent, SWT.NONE);
		content.setLayout(new FillLayout());
		graphViewer = new GraphViewer(content, SWT.NONE);

		makeFakeGraph();

		makeActions();
		hookContextMenu();
		contributeToActionBars();
	}

    private static DirectedGraph<String, DefaultEdge> createStringGraph()
    {
        DirectedGraph<String, DefaultEdge> g =
            new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);

        String v1 = "v1";
        String v2 = "v2";
        String v22 = "v22";
        String v3 = "v3";
        String v4 = "v4";
        String v5 = "v5";
        String v6 = "v6";

        // add the vertices
        g.addVertex(v1);
        g.addVertex(v2);
        g.addVertex(v22);
        g.addVertex(v3);
        g.addVertex(v4);
        g.addVertex(v5);
        g.addVertex(v6);

        // add edges to create a circuit
        g.addEdge(v1, v2);
        g.addEdge(v2, v22);
        g.addEdge(v22, v3);
        g.addEdge(v3, v4);
        g.addEdge(v2, v5);
        g.addEdge(v5, v6);
        g.addEdge(v6, v3);

        return g;
    }


	private void makeFakeGraph() {
		stringGraph = createStringGraph();
		graphViewer.setContentProvider(new ZestJGraphtNodeProvider());
		graphViewer.setLabelProvider(new ZestJgraphTLabelProvider());
		graphViewer.setInput(stringGraph.vertexSet());
		LayoutAlgorithm layout4 = new SugiyamaLayoutAlgorithm(SugiyamaLayoutAlgorithm.HORIZONTAL);
		graphViewer.setLayoutAlgorithm(layout4, true);
		graphViewer.applyLayout();
		layout4.applyLayout(true);
		ZoomContributionViewItem toolbar = new ZoomContributionViewItem(this);
		IActionBars bars = getViewSite().getActionBars();
		bars.getMenuManager().add(toolbar);
	}

	@TmfSignalHandler
	public void experimentSelected(
			final TmfExperimentSelectedSignal<? extends ITmfEvent> signal) {
		System.out.println("experiment selected " + signal.getExperiment());
		if (signal.getExperiment().equals(fSelectedExperiment)) {
            return;
        }

        final Thread thread = new Thread("ControlFlowView build") { //$NON-NLS-1$
            @Override
            public void run() {
                selectExperiment(signal.getExperiment());
            }
        };
        thread.start();
	}

	private void selectExperiment(final
			TmfExperiment<? extends ITmfEvent> experiment) {
		if (experiment == null)
			return;
		Job job = new Job("long running action") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Long running action", 100);
				for (int i = 0; i < 100; i++) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (monitor.isCanceled())
						break;
					monitor.worked(i);
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				TaskHierarchyView.this.fillContextMenu(manager);
			}
		});
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
	}

	private void fillContextMenu(IMenuManager manager) {
	}

	private void fillLocalToolBar(IToolBarManager manager) {
	}

	private void makeActions() {
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		content.setFocus();
	}

	@Override
	public AbstractZoomableViewer getZoomableViewer() {
		return graphViewer;
	}

}