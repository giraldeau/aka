package org.eclipse.linuxtools.lttng2.kernel.aka.views;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class CriticalPathView extends AbstractAKAView {

	public static final String ID = "org.eclipse.linuxtools.lttng2.kernel.aka.views.criticalpath"; //$NON-NLS-1$

	private TreeViewer tv;

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
	protected void updateData() {
		System.out.println("CriticalPathView updateData");
	}

}
