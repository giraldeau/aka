package org.eclipse.linuxtools.lttng2.kernel.aka.views;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.linuxtools.lttng2.kernel.aka.JobManager;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTimestamp;
import org.eclipse.linuxtools.tmf.core.event.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.event.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.signal.TmfRangeSynchSignal;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISelectionService;
import org.lttng.studio.collect.BinarySearch;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.kernel.TaskBlockingEntry;
import org.lttng.studio.model.kernel.TaskBlockings;
import org.lttng.studio.model.kernel.TimeInterval;
import org.lttng.studio.reader.handler.IModelKeys;

public class BlockingView extends AbstractAKAView {

	public static final String ID = "org.eclipse.linuxtools.lttng2.kernel.aka.views.blocking"; //$NON-NLS-1$

	private Composite composite;

	private TableViewer table;

	private final double marginFactor = 0.1;

	private boolean update;

	private Task fCurrentTask;

	private List<TaskBlockingEntry> fBlockingEntries;

	public BlockingView() {
		super(ID);
		update = false;
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());
		table = new TableViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER);
		table.setContentProvider(ArrayContentProvider.getInstance());
		Table t = table.getTable();
		t.setHeaderVisible(true);
		t.setLinesVisible(true);
		createColumns();
		table.setInput(null);
		table.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				// avoid nesting
				if (update)
					return;
				ISelection selection = table.getSelection();
				if (selection instanceof IStructuredSelection) {
					Object item = ((IStructuredSelection) selection).getFirstElement();
					if (item instanceof TaskBlockingEntry) {
						TimeInterval interval = ((TaskBlockingEntry) item).getInterval();
						long margin = (long) (interval.duration() * marginFactor);
		                final long startTime = interval.getStart() - margin;
		                final long endTime = interval.getEnd() + margin;
		                long center = (endTime - startTime) / 2 + startTime;
		                TmfTimeRange range = new TmfTimeRange(new CtfTmfTimestamp(startTime), new CtfTmfTimestamp(endTime));
		                TmfTimestamp time = new CtfTmfTimestamp(center);
		                broadcast(new TmfRangeSynchSignal(BlockingView.this, range, time));
					}
				}
			}
		});

		// FIXME: get the current ControlFlowEntry selected
		// FIXME: get current time range

	}

	private void createColumns() {
		TableViewerColumn col = new TableViewerColumn(table, SWT.NONE);
		col.getColumn().setWidth(200);
		col.getColumn().setText("Timestamp");
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
			    TaskBlockingEntry entry = (TaskBlockingEntry) element;
			    return String.format("%d", entry.getInterval().getStart());
			}
		});

		col = new TableViewerColumn(table, SWT.NONE);
		col.getColumn().setWidth(200);
		col.getColumn().setText("Duration");
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				TaskBlockingEntry entry = (TaskBlockingEntry) element;
				return String.format("%.9f", ((double)entry.getInterval().duration()) /  1000000000 );
			}
		});

		col = new TableViewerColumn(table, SWT.NONE);
		col.getColumn().setWidth(200);
		col.getColumn().setText("Syscall");
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				TaskBlockingEntry entry = (TaskBlockingEntry) element;
				CtfTmfEvent syscall = entry.getSyscall();
				if (syscall == null)
					return "unknown";
				return syscall.getEventName();
			}
		});

	}

	@Override
	public void setFocus() {
		composite.setFocus();
	}

	@Override
	public void dispose() {
		ISelectionService s = getSite().getWorkbenchWindow()
				.getSelectionService();
		s.removePostSelectionListener(selListener);
		JobManager.getInstance().removeListener(this);
		super.dispose();
	}

	@Override
	protected void updateDataSafe() {
		System.out.println("BlockingView updateData " + registry);
		if (registry == null)
				return;
		SystemModel system = registry.getModel(IModelKeys.SHARED, SystemModel.class);
		TaskBlockings blockings = registry.getModel(IModelKeys.SHARED, TaskBlockings.class);
		if (system == null || blockings == null)
			return;
		Task task = system.getTask(currentTid);
		// change input of table if task has changed
		if (task != null || task != fCurrentTask) {
			fCurrentTask = task;
			fBlockingEntries = blockings.getEntries().get(fCurrentTask);
			table.setInput(fBlockingEntries);
			table.refresh();
		}
		// scroll to nearest item according to current time
		TaskBlockingEntry entry = new TaskBlockingEntry();
		entry.getInterval().setStart(fCurrentTime);
		if (fBlockingEntries != null && !fBlockingEntries.isEmpty()) {
			int idx = BinarySearch.floor(fBlockingEntries, entry, TaskBlockingEntry.cmpStart);
			if (idx < 0)
				idx = BinarySearch.ceiling(fBlockingEntries, entry, TaskBlockingEntry.cmpStart);
			Object element = table.getElementAt(idx);
			System.out.println("selection index=" + idx + " " + element);
			if (element != null) {
				update = true;
				table.setSelection(new StructuredSelection(element), true);
				update = false;
				table.getTable().showSelection();
			}
		}
	}
}