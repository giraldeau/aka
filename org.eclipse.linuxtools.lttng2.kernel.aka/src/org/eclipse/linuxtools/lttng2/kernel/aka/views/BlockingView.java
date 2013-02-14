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
import org.eclipse.linuxtools.internal.lttng2.kernel.ui.views.controlflow.ControlFlowEntry;
import org.eclipse.linuxtools.lttng2.kernel.aka.JobListener;
import org.eclipse.linuxtools.lttng2.kernel.aka.JobManager;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTimestamp;
import org.eclipse.linuxtools.tmf.core.event.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.event.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.event.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.signal.TmfRangeSynchSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.signal.TmfTimeSynchSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ui.editors.ITmfTraceEditor;
import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.lttng.studio.collect.BinarySearch;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.kernel.TaskBlockingEntry;
import org.lttng.studio.model.kernel.TaskBlockings;
import org.lttng.studio.model.kernel.TimeInterval;
import org.lttng.studio.reader.handler.IModelKeys;

@SuppressWarnings("restriction")
public class BlockingView extends TmfView implements JobListener {

	public static final String ID = "org.eclipse.linuxtools.lttng2.kernel.aka.views.blocking"; //$NON-NLS-1$

	private Composite composite;

	private TableViewer table;

	private ITmfTrace fTrace;

	private long currentTid;

	private ModelRegistry registry;

	private final double marginFactor = 0.1;

	//private TmfTimeRange currentRange;

	private final JobManager manager;

	private List<? extends TaskBlockingEntry> fBlockingEntries;

	private Task fCurrentTask;

	private final Object fSyncObj = new Object();

	ISelectionListener selListener = new ISelectionListener() {
		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (part != BlockingView.this
					&& selection instanceof IStructuredSelection) {
				Object element = ((IStructuredSelection) selection)
						.getFirstElement();
				if (element instanceof ControlFlowEntry) {
					ControlFlowEntry entry = (ControlFlowEntry) element;
					setCurrentTid(entry.getThreadId());
				}
			}
		}
	};

	private long fCurrentTime;

	private boolean update;

	public BlockingView() {
		super(ID);
		manager = new JobManager();
		update = false;
	}

	@Override
	public void createPartControl(Composite parent) {
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

		// get selection events
		getSite().getWorkbenchWindow().getSelectionService()
				.addPostSelectionListener(selListener);

		// FIXME: get the current ControlFlowEntry selected
		// FIXME: get current time range

		// receive signal about processed trace
		manager.addListener(this);

    	IEditorPart editor = getSite().getPage().getActiveEditor();
    	if (editor instanceof ITmfTraceEditor) {
    	    ITmfTrace trace = ((ITmfTraceEditor) editor).getTrace();
    	    if (trace != null) {
    	    	traceSelected(new TmfTraceSelectedSignal(this, trace));
    	    }
    	}
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

	@TmfSignalHandler
	public void traceSelected(final TmfTraceSelectedSignal signal) {
		if (signal.getTrace() == fTrace)
			return;
		currentTid = -1;
		fTrace = signal.getTrace();
		synchronized(fSyncObj) {
			registry = null;
		}
		table.setInput(null);
		manager.launch(fTrace);
	}

	public void traceClosed(final TmfTraceClosedSignal signal) {
		synchronized(fSyncObj) {
			registry = null;
		}
		table.setInput(null);
	}

	@TmfSignalHandler
	public void synchToTime(final TmfTimeSynchSignal signal) {
		System.out.println("syncToTime " + signal.getCurrentTime());
		fCurrentTime = signal.getCurrentTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
		updateTable();
	}

	@TmfSignalHandler
	public void synchToRange(final TmfRangeSynchSignal signal) {
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
	public void ready(ITmfTrace trace) {
		// Our trace is not ready
		if (trace != fTrace) {
			return;
		}
		synchronized (fSyncObj) {
			registry = manager.getRegistry(fTrace);
		}
	}

	protected void updateTable() {
		SystemModel system;
		TaskBlockings blockings;
		synchronized(fSyncObj) {
			if (registry == null)
				return;
			system = registry.getModel(IModelKeys.SHARED, SystemModel.class);
			blockings = registry.getModel(IModelKeys.SHARED, TaskBlockings.class);
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

	protected void setCurrentTid(int threadId) {
		if (currentTid == threadId)
			return;
		System.out.println("setCurrentTid " + threadId);
		currentTid = threadId;
		updateTable();
	}
}