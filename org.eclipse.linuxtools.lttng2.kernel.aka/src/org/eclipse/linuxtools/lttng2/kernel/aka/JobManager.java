package org.eclipse.linuxtools.lttng2.kernel.aka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.lttng.studio.reader.AnalysisPhase;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.TimeLoadingListener;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;

public class JobManager {

	private static JobManager instance;
	private final List<JobListener> listeners;

	public JobManager() {
		listeners = new ArrayList<JobListener>();
	}

	public static JobManager getInstance() {
		if (instance == null)
			instance = new JobManager();
		return instance;
	}

	public synchronized Job launch(final ITmfTrace trace) {
		if (trace == null)
			return null;
		// Our analyzer only accepts CtfTmfTrace
		final AnalyzerThread thread = new AnalyzerThread();
		thread.setTrace(trace);

		Collection<AnalysisPhase> standardAnalysis = TraceEventHandlerFactory.makeStandardAnalysis();
		thread.addAllPhases(standardAnalysis);

		Job job = new Job("Advanced Kernel Analysis") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				TimeLoadingListener listener = new TimeLoadingListener("Trace processing",
						thread.getNumPhases(), monitor);
				thread.setListener(listener);
				thread.start();
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
					return Status.CANCEL_STATUS;
				}
				fireJobReady(thread);
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		return job;
	}

	public void addListener(JobListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public void removeListener(JobListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public void fireJobReady(AnalyzerThread thread) {
		synchronized (listeners) {
			for (JobListener listener: listeners) {
				listener.ready(thread);
			}
		}
	}

}
