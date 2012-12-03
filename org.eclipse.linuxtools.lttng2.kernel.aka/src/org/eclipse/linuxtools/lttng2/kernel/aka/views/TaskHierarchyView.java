package org.eclipse.linuxtools.lttng2.kernel.aka.views;


import java.util.Collection;
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
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.signal.TmfExperimentSelectedSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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
import org.eclipse.zest.layouts.algorithms.CompositeLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.SugiyamaLayoutAlgorithm;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.lttng.studio.model.graph.TaskHierarchyGraph;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.AnalysisPhase;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.TimeLoadingListener;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.reader.handler.ITraceEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;

/**
 * Basic graph view
 */

public class TaskHierarchyView extends TmfView implements IZoomableWorkbenchPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.eclipse.linuxtools.lttng2.kernel.aka.views.TaskHierarchyView";

	private TmfExperiment<ITmfEvent> fSelectedExperiment;

	private GraphViewer graphViewer;

	private Composite content;

	private TaskHierarchyGraph taskGraph;

	public class ZestJGraphtNodeProvider extends ArrayContentProvider implements IGraphEntityContentProvider {
		  @Override
		  public Object[] getConnectedTo(Object entity) {
		    if (entity instanceof Task) {
		      Task node = (Task) entity;
		      List<Task> neighbors = Graphs.successorListOf(taskGraph.getGraph(), node);
		      return neighbors.toArray();
		    }
		    throw new RuntimeException("Type not supported");
		  }
	}

	public class ZestJgraphTLabelProvider extends LabelProvider implements IConnectionStyleProvider {
		  @Override
		  public String getText(Object element) {
		    if (element instanceof Task) {
		      Task node = (Task) element;
		      return node.toString();
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

	private void makeFakeGraph() {
		graphViewer.setContentProvider(new ZestJGraphtNodeProvider());
		graphViewer.setLabelProvider(new ZestJgraphTLabelProvider());
		//LayoutAlgorithm layout4 = new SugiyamaLayoutAlgorithm(SugiyamaLayoutAlgorithm.HORIZONTAL);

		CompositeLayoutAlgorithm compositeLayoutAlgorithm = new CompositeLayoutAlgorithm(
				new LayoutAlgorithm[] { new SugiyamaLayoutAlgorithm(SugiyamaLayoutAlgorithm.HORIZONTAL) });
		graphViewer.setLayoutAlgorithm(compositeLayoutAlgorithm, true);
		graphViewer.applyLayout();
		compositeLayoutAlgorithm.applyLayout(true);
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

		// Our analyzer only accepts CtfTmfTrace
		final AnalyzerThread thread = new AnalyzerThread();
		ITmfTrace<? extends ITmfEvent>[] traces = experiment.getTraces();
		for (ITmfTrace<? extends ITmfEvent> trace: traces) {
			if (trace instanceof CtfTmfTrace) {
				thread.addTrace((CtfTmfTrace)trace);
			}
		}

		Collection<ITraceEventHandler> phase1 = TraceEventHandlerFactory.makeStatedump();
		Collection<ITraceEventHandler> phase2 = TraceEventHandlerFactory.makeFull();
		thread.addPhase(new AnalysisPhase("Statedump", phase1));
		thread.addPhase(new AnalysisPhase("Model recovery", phase2));

		Job job = new Job("long running action") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				TimeLoadingListener listener = new TimeLoadingListener("Trace processing",
						thread.getNumPhases(), monitor);
				thread.setListener(listener);
				thread.start();
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
					return Status.CANCEL_STATUS;
				}
				TraceReader reader = thread.getReader();
				TaskHierarchyGraph graph = reader.getRegistry().getModel(IModelKeys.SHARED, TaskHierarchyGraph.class);
				setTaskGraph(graph);
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	protected void setTaskGraph(TaskHierarchyGraph graph) {
		if (graph == null) {
			graph = new TaskHierarchyGraph();
		}
		this.taskGraph = graph;
		System.out.println("setTaskGraph() --> vertexSet.size() = " + taskGraph.getGraph().vertexSet().size());
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				graphViewer.setInput(taskGraph.getGraph().vertexSet());
			}
		});
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