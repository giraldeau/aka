package org.eclipse.linuxtools.lttng2.kernel.aka.views;

import java.util.HashMap;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jgrapht.traverse.AbstractGraphIterator;
import org.lttng.studio.model.graph.ClosestFirstCriticalPathAnnotation;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecGraph;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.graph.ForwardClosestIterator;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.handler.IModelKeys;

public class CriticalPathView extends AbstractAKAView {

	public static final String ID = "org.eclipse.linuxtools.lttng2.kernel.aka.views.criticalpath"; //$NON-NLS-1$

	private TreeViewer tv;

	private Task fCurrentTask;

	class CriticalPathContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return null;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}
	};

	class CriticalPathLabelProvider implements ILabelProvider {

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
			// TODO Auto-generated method stub
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub

		}

		@Override
		public Image getImage(Object element) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getText(Object element) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	public CriticalPathView() {
		super(ID);
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		parent.setLayout(new GridLayout(1, false));
		tv = new TreeViewer(parent);
		tv.setContentProvider(new CriticalPathContentProvider());
		tv.setLabelProvider(new CriticalPathLabelProvider());
	}

	@Override
	public void setFocus() {
		tv.getControl().setFocus();
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
			computeCriticalPath(graph, task);
		}
	}

	private void computeCriticalPath(ExecGraph graph, Task task) {
		ExecVertex head = graph.getStartVertexOf(task);
		if (!graph.getGraph().containsVertex(head)) {
			System.err.println("WARNING: head vertex is null for task " + task);
			System.out.println(graph.getVertexMap());
			return;
		}
		ClosestFirstCriticalPathAnnotation traversal = new ClosestFirstCriticalPathAnnotation(graph);
		AbstractGraphIterator<ExecVertex, ExecEdge> iter =
				new ForwardClosestIterator<ExecVertex, ExecEdge>(graph.getGraph(), head);
		iter.addTraversalListener(traversal);
		// FIXME: spawn a thread for background processing
		while (iter.hasNext() && !traversal.isDone())
			iter.next();
		HashMap<ExecEdge, Integer> map = traversal.getEdgeState();
		System.out.println(map);
	}

}
