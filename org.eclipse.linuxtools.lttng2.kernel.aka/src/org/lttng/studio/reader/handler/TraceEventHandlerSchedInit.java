package org.lttng.studio.reader.handler;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.kernel.Task.process_status;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

/*
 * Retrieve processes that are running at trace start, for each CPU
 */

public class TraceEventHandlerSchedInit extends TraceEventHandlerBase {

	SystemModel system;
	long[] cpus;
	int found;

	public TraceEventHandlerSchedInit() {
		super();
		hooks.add(new TraceHook("sched_switch"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
		found = 0;
		cpus = new long[reader.getNumCpus()];
		for (int i = 0; i < cpus.length; i++)
			cpus[i] = -1;
	}

	private void _update_task_state(long tid, process_status state) {
		Task task = system.getTask(tid);
		if (task != null) {
			task.setProcessStatus(state);
		} else {
			System.err.println("WARNING: unknown task tid=" + tid);
		};
	}

	public void handle_sched_switch(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		if (cpus[cpu] >= 0) {
			return;
		}

		// initial state of this CPU
		long prev = EventField.getLong(event, "prev_tid");
		//System.out.println("initial state: " + event.getTimestamp() + " " + event.getCPU() + " " + prev);
		system.setCurrentTid(cpu, prev);
		_update_task_state(prev, process_status.RUN);

		found++;
		cpus[cpu] = prev;
		// we found all initial CPUs state, let's stop
		if (found == cpus.length) {
			reader.cancel();
		}
	}

	@Override
	public void handleComplete(TraceReader reader) {
		// FIXME: debug mode should output this
		/*
		System.out.println("initial state");
		for (int i = 0; i < reader.getNumCpus(); i++) {
			system.setContextCPU(i);
			System.out.println("cpu " + i + " " + system.getTask(cpus[i]));
		}
		*/
	}

}
