package org.lttng.studio.reader.handler;

import java.util.HashMap;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.ArrayDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.StringDefinition;
import org.lttng.studio.model.kernel.FD;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

/*
 * Populate initial state of the system with statedump
 */

public class StatedumpEventHandler extends TraceEventHandlerBase {

	private SystemModel system;

	public StatedumpEventHandler() {
		super();
		this.hooks.add(new TraceHook("lttng_statedump_start"));
		this.hooks.add(new TraceHook("lttng_statedump_end"));
		this.hooks.add(new TraceHook("lttng_statedump_file_descriptor"));
		this.hooks.add(new TraceHook("lttng_statedump_process_state"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = ModelRegistry.getInstance().getOrCreateModel(reader, SystemModel.class);
		system.init(reader);
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

	public void handle_lttng_statedump_start(TraceReader reader, EventDefinition event) {
	}

	public void handle_lttng_statedump_end(TraceReader reader, EventDefinition event) {
		reader.cancel();
	}

	public void handle_lttng_statedump_file_descriptor(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		IntegerDefinition pid = (IntegerDefinition) def.get("_pid");
		StringDefinition filename = (StringDefinition) def.get("_filename");
		IntegerDefinition fd = (IntegerDefinition) def.get("_fd");
		Task task = system.getTask(pid.getValue());
		system.addTaskFD(task, new FD(fd.getValue(), filename.getValue()));
	}

	public void handle_lttng_statedump_process_state(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		IntegerDefinition pid = (IntegerDefinition) def.get("_pid");
		IntegerDefinition tid = (IntegerDefinition) def.get("_tid");
		IntegerDefinition ppid = (IntegerDefinition) def.get("_ppid");
		IntegerDefinition type = (IntegerDefinition) def.get("_type");
		IntegerDefinition mode = (IntegerDefinition) def.get("_mode");
		IntegerDefinition submode = (IntegerDefinition) def.get("_submode");
		IntegerDefinition status = (IntegerDefinition) def.get("_status");
		ArrayDefinition name = (ArrayDefinition) def.get("_name");

		Task task = new Task(tid.getValue());
		task.setPid(pid.getValue());
		task.setPpid(ppid.getValue());
		task.setExecutionMode(mode.getValue());
		task.setExecutionSubmode(submode.getValue());
		task.setProcessStatus(status.getValue());
		task.setThreadType(type.getValue());
		task.setName(name.toString());
		system.putTask(task);
	}
}
