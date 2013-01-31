package org.eclipse.linuxtools.lttng2.kernel.aka.views;


import java.util.List;

import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.IFigure;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.linuxtools.lttng2.kernel.aka.JobManager;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.IConnectionStyleProvider;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.lttng.studio.model.graph.TaskHierarchyGraph;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.handler.IModelKeys;

/**
 * Basic graph view
 */

public class TaskHierarchyView extends AbstractGraphView {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.eclipse.linuxtools.lttng2.kernel.aka.views.TaskHierarchyView";

	private TaskHierarchyGraph taskGraph;

	public class TaskNodeProvider extends ArrayContentProvider implements IGraphEntityContentProvider {
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

	public class TaskLabelProvider extends LabelProvider implements IConnectionStyleProvider {
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
		      //EntityConnectionData test = (EntityConnectionData) element;
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
		super.createPartControl(parent);
		graphViewer.setContentProvider(new TaskNodeProvider());
		graphViewer.setLabelProvider(new TaskLabelProvider());

		graphViewer.getGraphControl().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				IWorkbenchWindow window = getSite().getWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				try {
					page.showView(TaskExecutionGraphView.ID);
				} catch (PartInitException e1) {
					e1.printStackTrace();
				}
				IViewReference view = page.findViewReference(TaskExecutionGraphView.ID);
				TaskExecutionGraphView part = (TaskExecutionGraphView) view.getView(true);
				IStructuredSelection sel = (IStructuredSelection) graphViewer.getSelection();
				part.showTask((Task) sel.getFirstElement());
			}

		});
		makeActions();
		hookContextMenu();
		contributeToActionBars();
	}

	@Override
	public void ready(ITmfTrace trace) {
		ModelRegistry registry = JobManager.getInstance().getRegistry(trace);
		TaskHierarchyGraph graph = registry.getModel(IModelKeys.SHARED, TaskHierarchyGraph.class);
		setTaskHierarchyGraph(graph);
	}

	private void setTaskHierarchyGraph(TaskHierarchyGraph graph) {
		if (graph == null) {
			graph = new TaskHierarchyGraph();
		}
		this.taskGraph = graph;
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

}