package org.eclipse.linuxtools.lttng2.kernel.aka.views;


import java.util.List;

import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.IFigure;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.linuxtools.lttng2.kernel.aka.JobManager;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.IConnectionStyleProvider;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.CompositeLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.SugiyamaLayoutAlgorithm;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.Subgraph;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.graph.TaskExecutionGraph;
import org.lttng.studio.model.graph.TaskGraphExtractor;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.handler.IModelKeys;

/**
 * Basic graph view
 */

public class TaskExecutionGraphView extends AbstractGraphView {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.eclipse.linuxtools.lttng2.kernel.aka.views.TaskExecutionGraphView";

	private TaskExecutionGraph exeGraph;

	private Task task;

	public class ExecVertexNodeProvider extends ArrayContentProvider implements IGraphEntityContentProvider {
		  @Override
		  public Object[] getConnectedTo(Object entity) {
		    if (entity instanceof ExecVertex) {
		      ExecVertex node = (ExecVertex) entity;
		      List<ExecVertex> neighbors = Graphs.successorListOf(exeGraph.getGraph(), node);
		      return neighbors.toArray();
		    }
		    throw new RuntimeException("Type not supported");
		  }
	}

	public class ExecEdgeLabelProvider extends LabelProvider implements IConnectionStyleProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof ExecVertex) {
				ExecVertex node = (ExecVertex) element;
				return node.toString();
			}
			// Not called with the IGraphEntityContentProvider
			if (element instanceof ExecEdge) {
				ExecEdge edge = (ExecEdge) element;
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
	public TaskExecutionGraphView() {
		super("Task Execution View");
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		graphViewer.setContentProvider(new ExecVertexNodeProvider());
		graphViewer.setLabelProvider(new ExecEdgeLabelProvider());
		LayoutAlgorithm compositeLayout = new CompositeLayoutAlgorithm(new LayoutAlgorithm[] {
				new SugiyamaLayoutAlgorithm(SugiyamaLayoutAlgorithm.HORIZONTAL),
		//		new SpringLayoutAlgorithm(),
		});
		graphViewer.setLayoutAlgorithm(compositeLayout, true);

		makeActions();
		hookContextMenu();
		contributeToActionBars();
	}

	@Override
	public void ready(TmfExperiment<?> experiment) {
		ModelRegistry registry = JobManager.getInstance().getRegistry(experiment);
		TaskExecutionGraph graph = registry.getModel(IModelKeys.SHARED, TaskExecutionGraph.class);
		setTaskExecutionGraph(graph);
	}

	private void setTaskExecutionGraph(TaskExecutionGraph graph) {
		if (graph == null) {
			graph = new TaskExecutionGraph();
		}
		this.exeGraph = graph;
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				TaskExecutionGraphView.this.fillContextMenu(manager);
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

	public void showTask(Task task) {
		System.out.println("TaskExecutionGraph setTask " + task);
		this.task = task;
		ExecVertex startVertex = exeGraph.getStartVertexOf(task);
		ExecVertex endVertex = exeGraph.getEndVertexOf(task);
		final Subgraph<ExecVertex, ExecEdge, DirectedGraph<ExecVertex, ExecEdge>> subgraph =
				TaskGraphExtractor.getExecutionGraph(exeGraph, startVertex, endVertex);
		System.out.println("subgraph.vertexSet().size() " + subgraph.vertexSet().size());
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				graphViewer.setInput(subgraph.vertexSet());
			}
		});

	}

}