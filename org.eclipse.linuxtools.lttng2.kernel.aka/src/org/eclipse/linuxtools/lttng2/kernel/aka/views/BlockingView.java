package org.eclipse.linuxtools.lttng2.kernel.aka.views;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.linuxtools.internal.lttng2.kernel.ui.views.controlflow.ControlFlowEntry;
import org.eclipse.linuxtools.lttng2.kernel.aka.JobListener;
import org.eclipse.linuxtools.lttng2.kernel.aka.JobManager;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfTimeRange;
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
import org.lttng.studio.model.kernel.EventCounter;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.kernel.TaskBlockingEntry;
import org.lttng.studio.model.kernel.TaskBlockings;
import org.lttng.studio.reader.handler.IModelKeys;

@SuppressWarnings("restriction")
public class BlockingView extends TmfView implements JobListener {

	public static final String ID = "org.eclipse.linuxtools.lttng2.kernel.aka.views.blocking"; //$NON-NLS-1$

	private Composite composite;

	private TableViewer table;

	private ITmfTrace fTrace;

	private long current;

	private ModelRegistry registry;

	private TmfTimeRange currentRange;

	private final JobManager manager;

	ISelectionListener selListener = new ISelectionListener() {
		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (part != BlockingView.this
					&& selection instanceof IStructuredSelection) {
				Object element = ((IStructuredSelection) selection)
						.getFirstElement();
				if (element instanceof ControlFlowEntry) {
					ControlFlowEntry entry = (ControlFlowEntry) element;
					if (current == entry.getThreadId())
						return;
					current = entry.getThreadId();
					updateEntries();
				}
			}
		}
	};

	public BlockingView() {
		super(ID);
		manager = new JobManager();
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
		fTrace = signal.getTrace();
		registry = null;
		table.setInput(null);
		manager.launch(fTrace);
	}

	public void traceClosed(final TmfTraceClosedSignal signal) {
		// FIXME: synchronize
		registry = null;
		table.setInput(null);
	}

	@TmfSignalHandler
	public void synchToTime(final TmfTimeSynchSignal signal) {

	}

	@TmfSignalHandler
	public void synchToRange(final TmfRangeSynchSignal signal) {
		// FIXME: synchronize
		currentRange = signal.getCurrentRange();
		updateEntries();
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
		System.out.println("trace ready");
		registry = manager.getRegistry(fTrace);
		EventCounter model = registry.getModel(IModelKeys.SHARED, EventCounter.class);
		System.out.println(model.getCounter());
	}

	protected void updateEntries() {
		// FIXME: synchronize
		if (registry == null)
			return;
		SystemModel system = registry.getModel(IModelKeys.SHARED, SystemModel.class);
		TaskBlockings blockings = registry.getModel(IModelKeys.SHARED, TaskBlockings.class);
		if (system == null || blockings == null)
			return;
		Task task = system.getTask(current);
		if (task == null)
			return;
		List<? extends TaskBlockingEntry> list = blockings.getEntries().get(task);
		table.setInput(list);
		table.refresh();
	}

}