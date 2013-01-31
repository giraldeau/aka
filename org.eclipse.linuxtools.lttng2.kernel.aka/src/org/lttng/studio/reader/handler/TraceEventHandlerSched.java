package org.lttng.studio.reader.handler;

import java.util.HashMap;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.ArrayDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.StringDefinition;
import org.lttng.studio.model.kernel.CloneFlags;
import org.lttng.studio.model.kernel.FD;
import org.lttng.studio.model.kernel.FDSet;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.kernel.Task.execution_mode;
import org.lttng.studio.model.kernel.Task.process_status;
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
	private int schedSwitchUnkownTask;

	private AnalysisFilter filter;

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
		hooks.add(new TraceHook("sched_process_fork"));
		hooks.add(new TraceHook("sched_process_exit"));
		//hooks.add(new TraceHook(""));
		hooks.add(new TraceHook()); // get all events to check sys_* events
		hooks.add(new TraceHook("sys_execve"));
		hooks.add(new TraceHook("sys_clone"));
		hooks.add(new TraceHook("exit_syscall"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		filter = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, AnalysisFilter.class);
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
		schedSwitchUnkownTask = 0;
		evHistory = new HashMap<Long, EventData>();
	}

	private void _update_task_state(long tid, process_status state) {
		Task task = system.getTask(tid);
		if (task != null) {
			task.setProcessStatus(state);
		} else {
			schedSwitchUnkownTask++;
		};
	}

	public void handle_sched_switch(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		int cpu = event.getCPU();
		IntegerDefinition next = (IntegerDefinition) def.get("_next_tid");
		IntegerDefinition prev = (IntegerDefinition) def.get("_prev_tid");
		IntegerDefinition prev_state = (IntegerDefinition) def.get("_prev_state");
		
		system.setCurrentTid(cpu, next.getValue());

		_update_task_state(next.getValue(), process_status.RUN);
		
		// prev_state == 0 means runnable, thus waits for cpu
		if (prev_state.getValue() == 0) {
			_update_task_state(prev.getValue(), process_status.WAIT_CPU);
		} else {
			_update_task_state(prev.getValue(), process_status.WAIT_BLOCKED);
		}
	}

	public void handle_sched_process_fork(TraceReader reader, EventDefinition event) {
		// TODO: add child to parent's children list
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		IntegerDefinition parent = (IntegerDefinition) def.get("_parent_tid");
		IntegerDefinition child = (IntegerDefinition) def.get("_child_tid");
		ArrayDefinition name = (ArrayDefinition) def.get("_child_comm");
		Task task = new Task();
		task.setName(name.toString());
		task.setPid(parent.getValue());
		task.setPpid(parent.getValue());
		task.setTid(child.getValue());
		task.setStart(event.getTimestamp());
		system.putTask(task);

		// copy any outstanding event data to handle sys_clone exit
		EventData data = evHistory.get(parent.getValue());
		evHistory.put(child.getValue(), data);
	}

	public void handle_sched_process_exit(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		IntegerDefinition tid = (IntegerDefinition) def.get("_tid");
		Task task = system.getTask(tid.getValue());
		if (task == null)
			return;
		task.setEnd(event.getTimestamp());
		task.setProcessStatus(process_status.EXIT);
	}

	public void handle_all_event(TraceReader reader, EventDefinition event) {
		// ugly event matching, may clash
		if (!event.getDeclaration().getName().startsWith("sys_"))
			return;
		int cpu = event.getCPU();
		long tid = system.getCurrentTid(cpu);
		Task curr = system.getTask(tid);
		if (curr == null)
			return;
		curr.setExecutionMode(execution_mode.SYSCALL);
	}

	public void handle_sys_execve(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		int cpu = event.getCPU();
		long tid = system.getCurrentTid(cpu);
		String filename = ((StringDefinition) def.get("_filename")).toString();
		EventData data = new EventData();
		data.type = EventType.SYS_EXECVE;
		String cleanFile = StringHelper.unquote(filename);
		data.cmd = cleanFile;
		evHistory.put(tid, data);
	}

	public void handle_sys_clone(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		int cpu = event.getCPU();
		long tid = system.getCurrentTid(cpu);
		if (tid == 0) {
			long time = TraceReader.clockTime(event);
			System.err.println("WARNING: swapper clone cpu=" + cpu + " at " + time);
		}
		long flags = ((IntegerDefinition) def.get("_clone_flags")).getValue();
		EventData data = new EventData();
		data.flags = flags;
		data.type = EventType.SYS_CLONE;
		evHistory.put(tid, data); // tid of the clone caller
	}

	public void handle_exit_syscall(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		int cpu = event.getCPU();
		long tid = system.getCurrentTid(cpu);
		Task task = system.getTask(tid);
		if (task == null)
			return;

		// return to user-space
		task.setExecutionMode(execution_mode.USER_MODE);

		long ret = ((IntegerDefinition) def.get("_ret")).getValue();
		EventData ev = evHistory.remove(task.getTid());
		if (ev == null)
			return;
		switch (ev.type) {
		case SYS_EXECVE:
			if (ret == 0) {
				task.setName(ev.cmd);
				// check if this task needs to be monitored
				for (String c: filter.getCommands()) {
					if (ev.cmd.matches(c)) {
						filter.addTid(task.getTid());
						break;
					}
				}
			}
			break;
		case SYS_CLONE:
			if (ret > 0) { // child
				// handle filtering
				if (filter.isFollowChild() && filter.getTids().contains(task.getTid())) {
					filter.addTid(ret);
				}
				if (!CloneFlags.isFlagSet(ev.flags, CloneFlags.CLONE_FILES)) {
					// detach file descriptors from parent
					Task parent = system.getTask(task.getPpid());
					FDSet parentFDs = system.getFDSet(parent);
					FDSet childFDs = new FDSet();
					for (FD fd: parentFDs.getFDs()) {
						childFDs.addFD(new FD(fd));
					}
					system.setTaskFDSet(task, childFDs);
				}
				if (!CloneFlags.isFlagSet(ev.flags, CloneFlags.CLONE_THREAD)) {
					// Promote a thread to process
					task.setPid(task.getTid());
				}
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

	public int getSchedSwitchUnkownTask() {
		return schedSwitchUnkownTask;
	}

}
