package org.lttng.studio.reader.handler;

import java.util.HashMap;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.lttng.studio.model.kernel.CloneFlags;
import org.lttng.studio.model.kernel.FD;
import org.lttng.studio.model.kernel.FDSet;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.kernel.Task.execution_mode_enum;
import org.lttng.studio.model.kernel.Task.process_status_enum;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.utils.AnalysisFilter;
import org.lttng.studio.utils.StringHelper;

/*
 * Provides the current task running on a CPU according to scheduling events
 */

public class TraceEventHandlerSched extends TraceEventHandlerBase {

	/* Keep tmp info until corresponding sys_exit */
	public enum EventType { SYS_EXECVE, SYS_CLONE }
	public class EventData {
		public EventType type;
		public String cmd;
		public long flags;
	}

	HashMap<Long, EventData> evHistory;

	SystemModel system;

	private AnalysisFilter filter;

	private ALog log;

	/*
	 * sched_migrate_task:
	 * sched_process_exit:
	 * sched_process_fork:
	 * sched_process_free:
	 * sched_process_wait:
	 * sched_stat_runtime:
	 * sched_stat_sleep:
	 * sched_stat_wait:
	 * sched_switch:
	 * sched_wakeup:
	 * sched_wakeup_new:
	 */

	public TraceEventHandlerSched() {
		super();
		hooks.add(new TraceHook("sched_switch"));
		hooks.add(new TraceHook("sched_wakeup"));
		hooks.add(new TraceHook("sched_process_fork"));
		hooks.add(new TraceHook("sched_process_exit"));
		hooks.add(new TraceHook("sched_process_exec"));
		hooks.add(new TraceHook()); // get all events to check sys_* events
		//hooks.add(new TraceHook("sys_execve"));
		hooks.add(new TraceHook("sys_clone"));
		hooks.add(new TraceHook("exit_syscall"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		filter = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, AnalysisFilter.class);
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
		log = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, ALog.class);
		evHistory = new HashMap<Long, EventData>();
	}

	private Task createTask(long tid, long ts, String cmd) {
		Task task = new Task();
		task.setTid(tid);
		task.setStart(ts);
		task.setName(cmd);
		system.putTask(task);
		return task;
	}

	private void _update_task_state(long tid, process_status_enum state) {
		Task task = system.getTask(tid);

	}

	public void handle_sched_switch(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		if (system.getCurrentCPU() != cpu) {
			reader.cancel(new RuntimeException("ERROR: system.cpu != event.cpu"));
		}
		long next = EventField.getLong(event, "next_tid");
		long prev = EventField.getLong(event, "prev_tid");
		long prev_state = EventField.getLong(event, "prev_state");

		system.setCurrentTid(cpu, next);

		Task nextTask = system.getTask(next);
		if (nextTask == null) {
			String name = EventField.getString(event, "next_comm");
			nextTask = createTask(next, event.getTimestamp().getValue(), name);
			log.warning("sched_switch next task was null " + nextTask);
		}
		nextTask.setProcessStatus(process_status_enum.RUN);

		Task prevTask = system.getTask(prev);
		if (prevTask == null) {
			String name = EventField.getString(event, "prev_comm");
			prevTask = createTask(next, event.getTimestamp().getValue(), name);
			log.warning("sched_switch prev task was null " + prevTask);
		}
		process_status_enum status = prevTask.getProcessStatus();
		if (status != process_status_enum.RUN && status != process_status_enum.EXIT) {
			log.warning("prev task was not running " + prevTask + " " + event.getTimestamp());
		}
		// prev_state == 0 means runnable, thus waits for cpu
		if (prev_state == 0) {
			prevTask.setProcessStatus(process_status_enum.WAIT_CPU);
		} else {
			prevTask.setProcessStatus(process_status_enum.WAIT_BLOCKED);
		}
	}

	public void handle_sched_process_fork(TraceReader reader, CtfTmfEvent event) {
		// TODO: add child to parent's children list
		long parent = EventField.getLong(event, "parent_tid");
		long child = EventField.getLong(event, "child_tid");
		String name = EventField.getString(event, "child_comm");
		Task task = new Task();
		task.setName(name);
		task.setPid(parent);
		task.setPpid(parent);
		task.setTid(child);
		task.setStart(event.getTimestamp().getValue());
		system.putTask(task);

		// handle filtering
		Task parentTask = system.getTask(parent);
		if (parentTask != null && filter.isFollowChild() &&
				filter.getTids().contains(parentTask.getTid())) {
			filter.addTid(child);
		}

		// we know clone succeed, thus copy file descriptors according to flags
		EventData data = evHistory.remove(parent);
		if (data != null) {
			if (!CloneFlags.CLONE_FILES.isFlagSet(data.flags)) {
				// detach file descriptors from parent
				FDSet parentFDs = system.getFDSet(parentTask);
				FDSet childFDs = new FDSet();
				for (FD fd: parentFDs.getFDs()) {
					childFDs.addFD(new FD(fd));
				}
				system.setTaskFDSet(task, childFDs);
			}
			if (!CloneFlags.CLONE_THREAD.isFlagSet(data.flags)) {
				// Promote a thread to process
				task.setPid(task.getTid());
			}
		}
		// FIXME: in some cases, sys_clone is not matched to exit_syscall
		// thus let's make sure it returns in user mode
		task.setExecutionMode(execution_mode_enum.USER_MODE);
		task.setProcessStatus(process_status_enum.WAIT_FORK);
	}

	public void handle_sched_wakeup(TraceReader reader, CtfTmfEvent event) {
		long tid = EventField.getLong(event, "tid");
		Task target = system.getTask(tid);
		Task current = system.getTaskCpu(event.getCPU());
		if (target == null) {
			String name = EventField.getString(event, "comm");
			target = createTask(tid, event.getTimestamp().getValue(), name);
			target.setProcessStatus(process_status_enum.WAIT_BLOCKED);
			log.warning("sched_wakeup target was null " + event.getTimestamp().toString());
		}
		// spurious wakeup
		if (current != null && target.getTid() == current.getTid()) {
			log.warning("sched_wakeup SELF_WAKEUP " + target);
			return;
		}
		if (target.getProcessStatus() != Task.process_status_enum.WAIT_BLOCKED) {
			log.warning("sched_wakeup target " + target + " is not in WAIT_BLOCKED: " + target.getProcessStatus());
			return;
		}
		target.setProcessStatus(process_status_enum.WAIT_CPU);
	}

	public void handle_sched_process_exit(TraceReader reader, CtfTmfEvent event) {
		long tid = EventField.getLong(event, "tid");
		Task task = system.getTask(tid);
		if (task == null)
			return;
		task.setEnd(event.getTimestamp().getValue());
		task.setProcessStatus(process_status_enum.EXIT);
	}

	public void handle_sched_process_exec(TraceReader reader, CtfTmfEvent event) {
		String filename = EventField.getString(event, "filename");
		Task task = system.getTaskCpu(event.getCPU());
		task.setName(filename);

		// check if this task needs to be monitored
		for (String c: filter.getCommands()) {
			if (filename.matches(c)) {
				filter.addTid(task.getTid());
				break;
			}
		}
	}

	public void handle_all_event(TraceReader reader, CtfTmfEvent event) {
		// ugly event matching, may clash
		if (event.getEventName().startsWith("sys_")) {
			int cpu = event.getCPU();
			long tid = system.getCurrentTid(cpu);
			Task curr = system.getTask(tid);
			if (curr == null)
				return;
			curr.setExecutionMode(execution_mode_enum.SYSCALL);
		}
	}

	public void handle_sys_execve(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		long tid = system.getCurrentTid(cpu);
		String filename = EventField.getString(event, "filename");
		EventData data = new EventData();
		data.type = EventType.SYS_EXECVE;
		String cleanFile = StringHelper.unquote(filename);
		data.cmd = cleanFile;
		evHistory.put(tid, data);
	}

	public void handle_sys_clone(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		long tid = system.getCurrentTid(cpu);
		if (tid == 0) {
			//System.err.println("WARNING: swapper clone cpu=" + cpu + " at " + event.getTimestamp().getValue());
		}
		long flags = EventField.getLong(event, "clone_flags");
		EventData data = new EventData();
		data.flags = flags;
		data.type = EventType.SYS_CLONE;
		evHistory.put(tid, data); // tid of the clone caller
	}

	public void handle_exit_syscall(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		long tid = system.getCurrentTid(cpu);
		Task task = system.getTask(tid);
		if (task == null)
			return;

		// return to user-space
		task.setExecutionMode(execution_mode_enum.USER_MODE);
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

}
