package org.lttng.studio.reader.handler;

import java.util.HashMap;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDefinition;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.kernel.TaskBlockingEntry;
import org.lttng.studio.model.kernel.TaskBlockings;
import org.lttng.studio.model.kernel.WakeupInfo;
import org.lttng.studio.model.kernel.WakeupInfo.Type;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

/*
 * Detect blocking in a process
 */
public class TraceEventHandlerBlocking extends TraceEventHandlerBase {

	public long count;
	private TaskBlockings blockings;
	private HashMap<Task, EventDefinition> syscall;
	private WakeupInfo[] wakeup;
	private HashMap<Task, TaskBlockingEntry> latestBlockingMap;

	private SystemModel system;

	public TraceEventHandlerBlocking() {
		super();
		this.hooks.add(new TraceHook());
		this.hooks.add(new TraceHook("sched_switch"));
		this.hooks.add(new TraceHook("sched_wakeup"));
		this.hooks.add(new TraceHook("exit_syscall"));
		this.hooks.add(new TraceHook("softirq_entry"));
		this.hooks.add(new TraceHook("softirq_exit"));
		this.hooks.add(new TraceHook("hrtimer_expire_entry"));
		this.hooks.add(new TraceHook("hrtimer_expire_exit"));
		this.hooks.add(new TraceHook("inet_sock_local_in"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
		blockings = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, TaskBlockings.class);
		wakeup = new WakeupInfo[reader.getNumCpus()];
		syscall = new HashMap<Task, EventDefinition>();
		latestBlockingMap = new HashMap<Task, TaskBlockingEntry>();
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

	public void handle_softirq_entry(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		long vec = ((IntegerDefinition) def.get("_vec")).getValue();
		WakeupInfo info = new WakeupInfo();
		info.vec = vec;
		wakeup[event.getCPU()] = info;
	}

	public void handle_softirq_exit(TraceReader reader, EventDefinition event) {
		wakeup[event.getCPU()] = null;
	}

	public void handle_hrtimer_expire_entry(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		long timer = ((IntegerDefinition) def.get("_hrtimer")).getValue();
		WakeupInfo info = new WakeupInfo();
		info.type = Type.TIMER;
		info.timer = timer;
		wakeup[event.getCPU()] = info;
	}

	public void handle_hrtimer_expire_exit(TraceReader reader, EventDefinition event) {
		wakeup[event.getCPU()] = null;
	}

	public void handle_inet_sock_local_in(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		long sk = ((IntegerDefinition) def.get("_sk")).getValue();
		long seq = ((IntegerDefinition) def.get("_seq")).getValue();
		WakeupInfo info = wakeup[event.getCPU()];
		if (info != null) {
			info.type = WakeupInfo.Type.SOCK;
			info.sk = sk;
			info.seq = seq;
		}
	}

	public void handle_sched_switch(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		long state = ((IntegerDefinition) def.get("_prev_state")).getValue();
		long prevTid = ((IntegerDefinition) def.get("_prev_tid")).getValue();
		Task task = system.getTask(prevTid);
		// process is blocking
		if (state >= 1) {
			latestBlockingMap.put(task, new TaskBlockingEntry());
		}
	}

	public void handle_all_event(TraceReader reader, EventDefinition event) {
		Task task = system.getTaskCpu(event.getCPU());
		String name = event.getDeclaration().getName();
		// record the current system call
		if (name.startsWith("sys_")) {
			syscall.put(task, event);
		}
	}

	public void handle_exit_syscall(TraceReader reader, EventDefinition event) {
		Task task = system.getTaskCpu(event.getCPU());
		EventDefinition syscallEntry = syscall.remove(task);
		TaskBlockingEntry blockingEntry = latestBlockingMap.remove(task);
		if (syscallEntry != null && blockingEntry != null) {
			blockingEntry.getInterval().setStart(syscallEntry.getTimestamp());
			blockingEntry.getInterval().setEnd(event.getTimestamp());
		}
	}
	public void handle_sched_wakeup(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		long tid = ((IntegerDefinition) def.get("_tid")).getValue();
		Task blockedTask = system.getTask(tid);

		// spurious wake-up
		if (blockedTask.getProcessStatus() != Task.process_status.WAIT_BLOCKED) {
			wakeup[event.getCPU()] = null;
			return;
		}

		TaskBlockingEntry blocking = latestBlockingMap.get(blockedTask);
		if (blocking == null)
			return;
		//blocking.setInterval(new Interval(entry.getTimestamp(), exit.getTimestamp()));
		blocking.setSyscall(syscall.get(blockedTask));
		blocking.setTask(blockedTask);
		blocking.setWakeupInfo(wakeup[event.getCPU()]);
		blockings.getEntries().put(blockedTask, blocking);
	}

}