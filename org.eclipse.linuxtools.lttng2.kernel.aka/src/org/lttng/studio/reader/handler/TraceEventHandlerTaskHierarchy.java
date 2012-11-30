package org.lttng.studio.reader.handler;

import java.util.HashMap;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.StringDefinition;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.utils.StringHelper;

public class TraceEventHandlerTaskHierarchy  extends TraceEventHandlerBase {

	public enum EventType { SYS_EXECVE }
	public class EventData {
		public EventType type;
		public String cmd;
		public long flags;
	}

	HashMap<Long, EventData> evHistory;
	SystemModel system;

	public TraceEventHandlerTaskHierarchy() {
		super();
		hooks.add(new TraceHook("sched_process_fork"));
		hooks.add(new TraceHook("sys_execve"));
		hooks.add(new TraceHook("exit_syscall"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = ModelRegistry.getInstance().getOrCreateModel(reader, SystemModel.class);
		system.init(reader);
		evHistory = new HashMap<Long, EventData>();

		// TODO: init graph from statedump
	}

	public void handle_sched_process_fork(TraceReader reader, EventDefinition event) {
		// TODO: add child to parent's children list
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		IntegerDefinition parent = (IntegerDefinition) def.get("_parent_tid");
		IntegerDefinition child = (IntegerDefinition) def.get("_child_tid");
		//ArrayDefinition name = (ArrayDefinition) def.get("_child_comm");
		Task parentTask = system.getTask(parent.getValue());
		Task childTask = system.getTask(child.getValue());

		/*
		Node node = graph.createAndAppendNode(parentTask);
		node.setTimestamp(event.getTimestamp());
		node.setLabel("fork");

		node = graph.createAndAppendNode(childTask);
		node.setTimestamp(event.getTimestamp());
		node.setLabel("clone");
		*/
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

	public void handle_exit_syscall(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		int cpu = event.getCPU();
		long tid = system.getCurrentTid(cpu);
		Task task = system.getTask(tid);
		if (task == null)
			return;

		long ret = ((IntegerDefinition) def.get("_ret")).getValue();
		EventData ev = evHistory.remove(task.getTid());
		if (ev == null)
			return;
		switch (ev.type) {
		case SYS_EXECVE:
			if (ret == 0) {
				/*
				Node node = graph.createAndAppendNode(task);
				node.setTimestamp(event.getTimestamp());
				node.setLabel("exec");
				*/
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
