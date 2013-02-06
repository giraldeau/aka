package org.lttng.studio.reader.handler;

import java.util.HashMap;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.lttng.studio.model.kernel.FD;
import org.lttng.studio.model.kernel.FDSet;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class TraceEventHandlerFD extends TraceEventHandlerBase {

	/* Keep tmp info until corresponding sys_exit */
	public enum EventType { SYS_CLOSE, SYS_OPEN, SYS_DUP2 }
	public class EventData {
		public EventType type;
		public String name;
		public long fd;
		public long oldfd;
		public long newfd;
	}

	SystemModel system;

	HashMap<Long, EventData> evHistory;

	public TraceEventHandlerFD() {
		super();
		hooks.add(new TraceHook("sys_open"));
		hooks.add(new TraceHook("sys_close"));
		hooks.add(new TraceHook("sys_dup2"));
		hooks.add(new TraceHook("exit_syscall"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
		evHistory = new HashMap<Long, TraceEventHandlerFD.EventData>();
	}

	public void handle_sys_open(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		Task task = system.getTaskCpu(cpu);
		if (task == null)
			return;
		EventData ev = new EventData();
		ev.name = EventField.getString(event, "filename");
		ev.type = EventType.SYS_OPEN;
		evHistory.put(task.getTid(), ev);
	}

	public void handle_sys_close(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		system.getTaskCpu(cpu);
		Task task = system.getTaskCpu(cpu);
		if (task == null)
			return;
		EventData ev = new EventData();
		ev.fd = EventField.getLong(event, "fd");
		ev.type = EventType.SYS_CLOSE;
		evHistory.put(task.getTid(), ev);
	}

	public void handle_sys_dup2(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		system.getTaskCpu(cpu);
		Task task = system.getTaskCpu(cpu);
		if (task == null)
			return;
		EventData ev = new EventData();
		ev.oldfd = EventField.getLong(event, "oldfd");
		ev.newfd = EventField.getLong(event, "newfd");
		ev.type = EventType.SYS_DUP2;
		evHistory.put(task.getTid(), ev);
	}

	public void handle_exit_syscall(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		Task task = system.getTaskCpu(cpu);
		if (task == null)
			return;
		long ret = EventField.getLong(event, "ret");
		EventData ev = evHistory.remove(task.getTid());
		if (ev == null)
			return;
		switch (ev.type) {
		case SYS_CLOSE:
			if (ret == 0) {
				FDSet fdSet = system.getFDSet(task);
				FD fd = fdSet.getFD(ev.fd);
				fdSet.remove(fd);
			}
			break;
		case SYS_OPEN:
			if (ret >= 0) {
				system.addTaskFD(task, new FD(ret, ev.name));
			}
			break;
		case SYS_DUP2:
			if (ret >= 0) {
				// verify system call success
				assert(ret == ev.newfd);
				system.dup2FD(task, ev.oldfd,  ev.newfd);
			}
			break;
		default:
			break;
		}

	}

	@Override
	public void handleComplete(TraceReader reader) {
	}


}
