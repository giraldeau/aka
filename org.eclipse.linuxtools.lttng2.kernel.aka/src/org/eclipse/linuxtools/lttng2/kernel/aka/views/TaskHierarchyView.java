package org.eclipse.linuxtools.lttng2.kernel.aka.views;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.signal.TmfExperimentSelectedSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.SpringLayoutAlgorithm;

/**
 * Basic graph view
 */

public class TaskHierarchyView extends TmfView {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.eclipse.linuxtools.lttng2.kernel.aka.views.TaskHierarchyView";

	private TmfExperiment<ITmfEvent> fSelectedExperiment;

	private Label label;

	private Graph graph;

	private Composite content;

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
		content.setLayout(new GridLayout(1, false));
		label = new Label(content, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		label.setText("BIDON");

		graph = new Graph(content, SWT.NONE);
		graph.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		makeFakeGraph();

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(label, "org.eclipse.linuxtools.lttng2.kernel.aka.viewer");

		makeActions();
		hookContextMenu();
		contributeToActionBars();
	}

	private void makeFakeGraph() {
		GraphNode node1 = new GraphNode(graph, SWT.NONE, "Jim");
		GraphNode node2 = new GraphNode(graph, SWT.NONE, "Jack");
		GraphNode node3 = new GraphNode(graph, SWT.NONE, "Joe");
		GraphNode node4 = new GraphNode(graph, SWT.NONE, "Bill");
		new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, node1,
				node2);
		// Lets have a dotted graph connection
		new GraphConnection(graph, ZestStyles.CONNECTIONS_DOT, node2, node3);
		// Standard connection
		new GraphConnection(graph, SWT.NONE, node3, node1);
		// Change line color and line width
		GraphConnection graphConnection = new GraphConnection(graph, SWT.NONE,
				node1, node4);
		graphConnection.changeLineColor(content.getDisplay().getSystemColor(
				SWT.COLOR_GREEN));
		// Also set a text
		graphConnection.setText("This is a text");
		graphConnection.setHighlightColor(content.getDisplay().getSystemColor(
				SWT.COLOR_RED));
		graphConnection.setLineWidth(3);

		graph.setLayoutAlgorithm(new SpringLayoutAlgorithm(
				LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
		// Selection listener on graphConnect or GraphNode is not supported
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=236528
		graph.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				System.out.println(e);
			}

		});
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
		label.setFocus();
	}

}