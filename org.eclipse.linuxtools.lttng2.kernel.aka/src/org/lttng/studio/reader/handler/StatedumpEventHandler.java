package org.lttng.studio.reader.handler;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.lttng.studio.model.kernel.FD;
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
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

	public void handle_lttng_statedump_start(TraceReader reader, CtfTmfEvent event) {
	}

	public void handle_lttng_statedump_end(TraceReader reader, CtfTmfEvent event) {
		reader.cancel();
	}

	public void handle_lttng_statedump_file_descriptor(TraceReader reader, CtfTmfEvent event) {
		long pid = EventField.getLong(event, "pid");
		String filename = EventField.getString(event, "filename");
		long fd = EventField.getLong(event, "fd");
		Task task = system.getTask(pid);
		system.addTaskFD(task, new FD(fd, filename));
	}

	public void handle_lttng_statedump_process_state(TraceReader reader, CtfTmfEvent event) {
		long pid = EventField.getLong(event, "pid");
		long tid = EventField.getLong(event, "tid");
		long ppid = EventField.getLong(event, "ppid");
		long type = EventField.getLong(event, "type");
		long mode = EventField.getLong(event, "mode");
		long submode = EventField.getLong(event, "submode");
		long status = EventField.getLong(event, "status");

		String name = EventField.getString(event, "name");

		Task task = new Task(tid);
		task.setStart(reader.getTimeRange().getStartTime().getValue());
		task.setPid(pid);
		task.setPpid(ppid);
		task.setExecutionMode(mode);
		task.setExecutionSubmode(submode);
		task.setProcessStatus(status);
		task.setThreadType(type);
		task.setName(name);
		system.putTask(task);
	}
}
