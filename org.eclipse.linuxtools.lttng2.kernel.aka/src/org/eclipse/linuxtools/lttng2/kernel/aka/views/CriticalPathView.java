package org.eclipse.linuxtools.lttng2.kernel.aka.views;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.lttng.studio.model.graph.CriticalPathStats;
import org.lttng.studio.model.graph.ExecGraph;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.graph.Span;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.handler.IModelKeys;

public class CriticalPathView extends AbstractAKAView {

	public static final String ID = "org.eclipse.linuxtools.lttng2.kernel.aka.views.criticalpath"; //$NON-NLS-1$

	private TreeViewer treeViewer;

	private Task fCurrentTask;

	private Span fSpanRoot;

	private Composite composite;

	public static final double NANO = 1000000000.0;
	public static final double NANOINV = 0.000000001;
	public static final String spanTimeFmt = "%8.9f";
	public static final String spanRelFmt = "%8.3f";

	private class SpanContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof Span) {
				return ((Span)parentElement).getChildren().toArray();
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof Span)
				return ((Span)element).getParent();
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof Span)
				return ((Span)element).getChildren().size() > 0;
			return false;
		}

		@Override
		public Object[] getElements(Object root) {
			return getChildren(root);
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	class TableLabelProvider implements ITableLabelProvider {

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			String str = "";
			if (element instanceof Span) {
				Span span = (Span) element;
				switch (columnIndex) {
				case 0: // Object
					str = span.getOwner().toString();
					break;
				case 1: // Self time
					str = String.format(spanTimeFmt, span.getSelfTime() * NANOINV);
					break;
				case 2: // Inc. time
					str = String.format(spanTimeFmt, span.getTotalTime() * NANOINV);
					break;
				case 3: // % Self
					double self = ((double)span.getSelfTime()) / fSpanRoot.getTotalTime() * 100.0;
					str = String.format(spanRelFmt, self);
					break;
				case 4: // % Inc.
					double incl = ((double)span.getTotalTime()) / fSpanRoot.getTotalTime() * 100.0;
					str = String.format(spanRelFmt, incl);
					break;
				default:
					break;
				}
			}

			return str;
		}

	}

	public CriticalPathView() {
		super(ID);
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());

		Tree spanTree = new Tree(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		spanTree.setHeaderVisible(true);
		spanTree.setLinesVisible(true);

		treeViewer = new TreeViewer(spanTree);
		createColumns(spanTree);
		treeViewer.setContentProvider(new SpanContentProvider());
		treeViewer.setLabelProvider(new TableLabelProvider());

		/*
		comparator = new SpanColumnSorter();
		table.setComparator(comparator);
		*/
	}

	private void setSpanRoot(Span root) {
		this.fSpanRoot = root;
		treeViewer.setInput(root);
		treeViewer.expandAll();
	}

	private void createColumns(Tree tree) {
		TreeColumn col1 = new TreeColumn(tree, SWT.LEFT);
		col1.setAlignment(SWT.LEFT);
		col1.setText("Object");
		col1.setWidth(200);

		TreeColumn col2 = new TreeColumn(tree, SWT.RIGHT);
		col2.setAlignment(SWT.RIGHT);
		col2.setText("Self time (sec)");
		col2.setWidth(200);

		TreeColumn col3 = new TreeColumn(tree, SWT.RIGHT);
		col3.setAlignment(SWT.RIGHT);
		col3.setText("Inc. time (sec)");
		col3.setWidth(200);

		TreeColumn col4 = new TreeColumn(tree, SWT.RIGHT);
		col4.setAlignment(SWT.RIGHT);
		col4.setText("% Self");
		col4.setWidth(200);

		TreeColumn col5 = new TreeColumn(tree, SWT.RIGHT);
		col5.setAlignment(SWT.RIGHT);
		col5.setText("% Inc.");
		col5.setWidth(200);
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	@Override
	protected void updateDataSafe() {
		System.out.println("CriticalPathView updateData " + registry);
		if (registry == null)
			return;
		SystemModel system = registry.getModel(IModelKeys.SHARED, SystemModel.class);
		ExecGraph graph = registry.getModel(IModelKeys.SHARED, ExecGraph.class);
		if (system == null || graph == null)
			return;
		Task task = system.getTask(currentTid);
		// change input if task has changed
		if (task != null || task != fCurrentTask) {
			fCurrentTask = task;
			computeCriticalPath(graph, task);
		}
	}

	private void computeCriticalPath(ExecGraph graph, Task task) {
		ExecVertex head = graph.getStartVertexOf(task);
		if (!graph.getGraph().containsVertex(head)) {
			System.err.println("WARNING: head vertex is null for task " + task);
			return;
		}
		Span root = CriticalPathStats.compile(graph, head);
		setSpanRoot(root);
	}

}
