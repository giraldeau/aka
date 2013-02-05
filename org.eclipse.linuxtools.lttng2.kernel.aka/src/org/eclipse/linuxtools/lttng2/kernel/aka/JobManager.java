package org.eclipse.linuxtools.lttng2.kernel.aka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.reader.AnalysisPhase;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.TimeLoadingListener;
import org.lttng.studio.reader.handler.ITraceEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;

public class JobManager {

	private static JobManager instance;
	private final HashMap<ITmfTrace, ModelRegistry> registryMap;
	private final HashMap<ITmfTrace, Job> jobMap;
	private final List<JobListener> listeners;

	private JobManager() {
		registryMap = new HashMap<ITmfTrace, ModelRegistry>();
		jobMap = new HashMap<ITmfTrace, Job>();
		listeners = new ArrayList<JobListener>();
	}

	public static JobManager getInstance() {
		if (instance == null)
			instance = new JobManager();
		return instance;
	}

	public synchronized void launch(final ITmfTrace trace) {
		if (trace == null)
			return;

		// Our analyzer only accepts CtfTmfTrace
		final AnalyzerThread thread = new AnalyzerThread();
		if (trace instanceof CtfTmfTrace) {
			thread.addTrace((CtfTmfTrace)trace);
		}

		Collection<ITraceEventHandler> phase1 = TraceEventHandlerFactory.makeStatedump();
		Collection<ITraceEventHandler> phase2 = TraceEventHandlerFactory.makeFull();
		thread.addPhase(new AnalysisPhase("Statedump", phase1));
		thread.addPhase(new AnalysisPhase("Model recovery", phase2));

		Job job = new Job("long running action") {
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
				registryMap.put(trace, thread.getReader().getRegistry());
				fireJobReady(trace);
				synchronized (jobMap) {
					jobMap.remove(trace);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);

		synchronized (jobMap) {
			if (jobMap.containsKey(trace))
				return;
			job.schedule();
			jobMap.put(trace, job);
		}
	}

	public void addListener(JobListener listener) {
		listeners.add(listener);
	}

	public void removeListener(JobListener listener) {
		listeners.remove(listener);
	}

	public void fireJobReady(ITmfTrace trace) {
		for (JobListener listener: listeners) {
			listener.ready(trace);
		}
	}

	public ModelRegistry getRegistry(ITmfTrace trace) {
		return registryMap.get(trace);
	}

}
