package org.eclipse.linuxtools.lttng2.kernel.aka.views;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.linuxtools.internal.lttng2.kernel.ui.views.controlflow.ControlFlowEntry;
import org.eclipse.linuxtools.lttng2.kernel.aka.JobListener;
import org.eclipse.linuxtools.lttng2.kernel.aka.JobManager;
import org.eclipse.linuxtools.tmf.core.event.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.event.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.signal.TmfRangeSynchSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.signal.TmfTimeSynchSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ui.editors.ITmfTraceEditor;
import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.lttng.studio.model.kernel.ModelRegistry;

@SuppressWarnings("restriction")
public abstract class AbstractAKAView extends TmfView implements JobListener {

	protected ITmfTrace fTrace;
	protected long currentTid;
	protected long fCurrentTime;
	private TmfTimeRange fCurrentRange;
	protected ModelRegistry registry;
	protected final JobManager manager;
	protected final Object fSyncObj = new Object();

	protected ISelectionListener selListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				if (part != AbstractAKAView.this
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

	public AbstractAKAView(String viewName) {
		super(viewName);
		manager = JobManager.getInstance();
		// receive signal about processed trace
		manager.addListener(this);
	}

	protected void setCurrentTid(int threadId) {
		if (currentTid == threadId)
			return;
		System.out.println("setCurrentTid " + threadId);
		currentTid = threadId;
		updateData();
	}

	@Override
	public void createPartControl(Composite parent) {
		// get selection events
		getSite().getWorkbenchWindow().getSelectionService()
				.addPostSelectionListener(selListener);

    	IEditorPart editor = getSite().getPage().getActiveEditor();
    	if (editor instanceof ITmfTraceEditor) {
    	    ITmfTrace trace = ((ITmfTraceEditor) editor).getTrace();
    	    if (trace != null) {
    	    	traceSelected(new TmfTraceSelectedSignal(this, trace));
    	    }
    	}
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
		updateData();
		manager.launch(fTrace);
	}

	@TmfSignalHandler
	public void traceClosed(final TmfTraceClosedSignal signal) {
		synchronized(fSyncObj) {
			registry = null;
		}
		updateData();
	}

	@TmfSignalHandler
	public void synchToTime(final TmfTimeSynchSignal signal) {
		fCurrentTime = signal.getCurrentTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
		updateData();
	}

	@TmfSignalHandler
	public void synchToRange(final TmfRangeSynchSignal signal) {
		fCurrentRange = signal.getCurrentRange();
		updateData();
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

	private void updateData() {
		synchronized (fSyncObj) {
			updateDataSafe();
		}
	}

	protected void updateDataSafe() {
	}

}
