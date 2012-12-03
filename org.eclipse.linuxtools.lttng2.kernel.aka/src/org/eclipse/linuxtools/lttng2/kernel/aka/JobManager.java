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
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.reader.AnalysisPhase;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.TimeLoadingListener;
import org.lttng.studio.reader.handler.ITraceEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;

public class JobManager {

	private static JobManager instance;
	private final HashMap<TmfExperiment<?>, ModelRegistry> registryMap;
	private final HashMap<TmfExperiment<?>, Job> jobMap;
	private final List<JobListener> listeners;

	private JobManager() {
		registryMap = new HashMap<TmfExperiment<?>, ModelRegistry>();
		jobMap = new HashMap<TmfExperiment<?>, Job>();
		listeners = new ArrayList<JobListener>();
	}

	public static JobManager getInstance() {
		if (instance == null)
			instance = new JobManager();
		return instance;
	}

	public synchronized void launch(final TmfExperiment<?> experiment) {
		if (experiment == null)
			return;

		if (jobMap.containsKey(experiment))
			return;

		// Our analyzer only accepts CtfTmfTrace
		final AnalyzerThread thread = new AnalyzerThread();
		ITmfTrace<? extends ITmfEvent>[] traces = experiment.getTraces();
		for (ITmfTrace<? extends ITmfEvent> trace: traces) {
			if (trace instanceof CtfTmfTrace) {
				thread.addTrace((CtfTmfTrace)trace);
			}
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
				registryMap.put(experiment, thread.getReader().getRegistry());
				fireJobReady(experiment);
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		jobMap.put(experiment, job);
	}

	public void addListener(JobListener listener) {
		listeners.add(listener);
	}

	public void removeListener(JobListener listener) {
		listeners.remove(listener);
	}

	public void fireJobReady(TmfExperiment<?> experiment) {
		for (JobListener listener: listeners) {
			listener.ready(experiment);
		}
	}

	public ModelRegistry getRegistry(TmfExperiment<?> experiment) {
		return registryMap.get(experiment);
	}

}
