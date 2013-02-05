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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
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

	private Task current;

	ISelectionListener selListener = new ISelectionListener() {
		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (part != BlockingView.this
					&& selection instanceof IStructuredSelection) {
				Object element = ((IStructuredSelection) selection)
						.getFirstElement();
				if (element instanceof ControlFlowEntry) {
					ControlFlowEntry entry = (ControlFlowEntry) element;
					ModelRegistry registry = JobManager.getInstance().getRegistry(fTrace);
					SystemModel system = registry.getModel(IModelKeys.SHARED, SystemModel.class);
					TaskBlockings blockings = registry.getModel(IModelKeys.SHARED, TaskBlockings.class);
					Task task = system.getTask(entry.getThreadId());
					if (task == current)
						return;
					current = task;
					List<TaskBlockingEntry> list = blockings.getEntries().get(task);
					table.setInput(list);
				}
			}
		}
	};

	public BlockingView() {
		super(ID);
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

		table.setInput(null);

		// get selection events
		getSite().getWorkbenchWindow().getSelectionService()
				.addPostSelectionListener(selListener);

		// receive signal about processed trace
		JobManager.getInstance().addListener(this);

        IEditorPart editor = getSite().getPage().getActiveEditor();
        if (editor instanceof ITmfTraceEditor) {
            ITmfTrace trace = ((ITmfTraceEditor) editor).getTrace();
            if (trace != null) {
                traceSelected(new TmfTraceSelectedSignal(this, trace));
            }
        }
	}

	public void createColumns(Composite parent) {
		TableViewerColumn col = new TableViewerColumn(table, SWT.NONE);
		col.getColumn().setWidth(200);
		col.getColumn().setText("Col 1");
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				String s = (String) element;
				return s;
			}
		});
	}

	@TmfSignalHandler
	public void traceSelected(final TmfTraceSelectedSignal signal) {
		if (signal.getTrace() == fTrace)
			return;
		fTrace = signal.getTrace();
		table.setInput(null);
		table.getControl().setEnabled(false);
		JobManager.getInstance().launch(fTrace);
	}

	public void traceClosed(final TmfTraceClosedSignal signal) {
		table.setInput(null);
	}

	@TmfSignalHandler
	public void synchToTime(final TmfTimeSynchSignal signal) {

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
		Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
            	table.getControl().setEnabled(true);
            }
		});
	}

}
