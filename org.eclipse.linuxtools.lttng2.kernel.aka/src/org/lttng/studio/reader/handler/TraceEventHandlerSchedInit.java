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
	int[] cpus;
	int found;
	private static final int UNKNOWN = 0;
	private static final int FOUND = 1;

	public TraceEventHandlerSchedInit() {
		super();
		hooks.add(new TraceHook("sched_switch"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
		found = 0;
		cpus = new int[reader.getNumCpus()];
		for (int i = 0; i < cpus.length; i++)
			cpus[i] = UNKNOWN;
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
		if (cpus[cpu] == FOUND) {
			return;
		}

		long prev = EventField.getLong(event, "prev_tid");
		long prev_state = EventField.getLong(event, "prev_state");

		system.setCurrentTid(cpu, prev);

		// prev_state == 0 means runnable, thus waits for cpu
		if (prev_state == 0) {
			_update_task_state(prev, process_status.WAIT_CPU);
		} else {
			_update_task_state(prev, process_status.WAIT_BLOCKED);
		}
		found++;
		cpus[cpu] = FOUND;
		// we found all initial CPUs state, let's stop
		if (found == cpus.length) {
			reader.cancel();
		}
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

}
