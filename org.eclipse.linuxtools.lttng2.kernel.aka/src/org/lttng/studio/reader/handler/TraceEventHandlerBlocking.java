package org.lttng.studio.reader.handler;

import java.util.HashMap;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.kernel.TaskBlockingEntry;
import org.lttng.studio.model.kernel.TaskBlockings;
import org.lttng.studio.model.kernel.WakeupInfo;
import org.lttng.studio.model.kernel.WakeupInfo.Type;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.utils.AnalysisFilter;

/*
 * Detect blocking in a process
 */
public class TraceEventHandlerBlocking extends TraceEventHandlerBase {

	public long count;
	private TaskBlockings blockings;
	private HashMap<Task, CtfTmfEvent> syscall;
	private WakeupInfo[] wakeup;
	private HashMap<Task, TaskBlockingEntry> latestBlockingMap;

	private SystemModel system;
	private AnalysisFilter filter;

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
		filter = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, AnalysisFilter.class);
		blockings = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, TaskBlockings.class);
		wakeup = new WakeupInfo[reader.getNumCpus()];
		syscall = new HashMap<Task, CtfTmfEvent>();
		latestBlockingMap = new HashMap<Task, TaskBlockingEntry>();
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

	public void handle_softirq_entry(TraceReader reader, CtfTmfEvent event) {
		long vec = EventField.getLong(event, "vec");
		WakeupInfo info = new WakeupInfo();
		info.vec = vec;
		wakeup[event.getCPU()] = info;
	}

	public void handle_softirq_exit(TraceReader reader, CtfTmfEvent event) {
		wakeup[event.getCPU()] = null;
	}

	public void handle_hrtimer_expire_entry(TraceReader reader, CtfTmfEvent event) {
		long timer = EventField.getLong(event, "hrtimer");
		WakeupInfo info = new WakeupInfo();
		info.type = Type.TIMER;
		info.timer = timer;
		wakeup[event.getCPU()] = info;
	}

	public void handle_hrtimer_expire_exit(TraceReader reader, CtfTmfEvent event) {
		wakeup[event.getCPU()] = null;
	}

	public void handle_inet_sock_local_in(TraceReader reader, CtfTmfEvent event) {
		long sk = EventField.getLong(event, "sk");
		long seq = EventField.getLong(event, "seq");
		WakeupInfo info = wakeup[event.getCPU()];
		if (info != null) {
			info.type = WakeupInfo.Type.SOCK;
			info.sk = sk;
			info.seq = seq;
		}
	}

	public void handle_sched_switch(TraceReader reader, CtfTmfEvent event) {
		long state = EventField.getLong(event, "prev_state");
		long prevTid = EventField.getLong(event, "prev_tid");
		long nextTid = EventField.getLong(event, "next_tid");

		// task is blocking
		if (state >= 1) {
			Task prevTask = system.getTask(prevTid);
			TaskBlockingEntry entry = new TaskBlockingEntry();
			entry.getInterval().setStart(event.getTimestamp().getValue());
			latestBlockingMap.put(prevTask, entry);
			/*
			if (filter.containsTaskTid(prevTask)) {
				System.out.println("sched_switch task is blocking " + prevTask + " " + event.getTimestamp());
			}
			*/
		}
		// task may be scheduled after wake-up
		Task nextTask = system.getTask(nextTid);
		TaskBlockingEntry entry = latestBlockingMap.remove(nextTask);
		if (entry != null) {
			entry.getInterval().setEnd(event.getTimestamp().getValue());
			/*
			if (filter.containsTaskTid(nextTask))
				System.out.println("sched_switch task is waking up " + nextTask + " " + event.getTimestamp());
			*/
		}
	}

	public void handle_all_event(TraceReader reader, CtfTmfEvent event) {
		Task task = system.getTaskCpu(event.getCPU());
		String name = event.getEventName();
		// record the current system call
		if (name.startsWith("sys_")) {
			syscall.put(task, event);
		}
	}

	public void handle_exit_syscall(TraceReader reader, CtfTmfEvent event) {
		Task task = system.getTaskCpu(event.getCPU());
		syscall.remove(task);
	}

	public void handle_sched_wakeup(TraceReader reader, CtfTmfEvent event) {
 		long tid = EventField.getLong(event, "tid");
		Task blockedTask = system.getTask(tid);

		if (blockedTask == null)
			return;

		// spurious wake-up
		if (blockedTask.getProcessStatus() != Task.process_status_enum.WAIT_BLOCKED) {
			wakeup[event.getCPU()] = null;
			return;
		}

		TaskBlockingEntry blocking = latestBlockingMap.get(blockedTask);
		if (blocking == null)
			return;
		/*
		if (filter.containsTaskTid(blockedTask))
			System.out.println("sched_wakeup " + blockedTask + " " + blockedTask.getProcessStatus() + " " + blocking);
		*/
		blocking.setSyscall(syscall.get(blockedTask));
		blocking.setTask(blockedTask);
		blocking.setWakeupInfo(wakeup[event.getCPU()]);
		blockings.getEntries().put(blockedTask, blocking);
	}

}