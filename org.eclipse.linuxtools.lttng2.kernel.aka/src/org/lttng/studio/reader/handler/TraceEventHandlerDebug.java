package org.lttng.studio.reader.handler;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.utils.AnalysisFilter;

/*
 * Detect blocking in a process
 */
public class TraceEventHandlerDebug extends TraceEventHandlerBase {

	private SystemModel system;
	private AnalysisFilter filter;

	public TraceEventHandlerDebug() {
		super();
		this.hooks.add(new TraceHook());
		this.hooks.add(new TraceHook("sched_switch"));
		this.hooks.add(new TraceHook("sched_wakeup"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
		filter = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, AnalysisFilter.class);
		for (Long tid: filter.getTids()) {
			Task task = system.getTask(tid);
			//System.out.println(task + " " + task.getExecutionMode() + " " + task.getProcessStatus());
		}
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

	public void handle_sched_switch(TraceReader reader, CtfTmfEvent event) {
		long state = EventField.getLong(event, "prev_state");
		long prevTid = EventField.getLong(event, "prev_tid");
		long nextTid = EventField.getLong(event, "next_tid");
		Task prevTask = system.getTask(prevTid);
		Task nextTask = system.getTask(nextTid);
		if (filter.containsTaskTid(prevTask) || filter.containsTaskTid(nextTask)) {
			//System.out.println("sched_switch task " + prevTid + " -> " + nextTid);
			//System.out.println(prevTask + " " + prevTask.getExecutionMode() + " " + prevTask.getProcessStatus());
			//System.out.println(nextTask + " " + nextTask.getExecutionMode() + " " + nextTask.getProcessStatus());
		}
	}

	public void handle_all_event(TraceReader reader, CtfTmfEvent event) {
	}

	public void handle_exit_syscall(TraceReader reader, CtfTmfEvent event) {
	}

	public void handle_sched_wakeup(TraceReader reader, CtfTmfEvent event) {
		long tid = EventField.getLong(event, "tid");
		Task blockedTask = system.getTask(tid);

		if (blockedTask == null) {
			//System.err.println("WARNING: wakup of unkown task " + tid);
			return;
		}

		if (filter.containsTaskTid(blockedTask)) {
			//System.out.println("sched_wakeup task " + tid);
			//System.out.println(blockedTask + " " + blockedTask.getExecutionMode() + " " + blockedTask.getProcessStatus());
		}
	}

}