package org.lttng.studio.reader.handler;

import java.util.HashMap;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDefinition;
import org.lttng.studio.model.kernel.Inet4Sock;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class StatedumpInetSockEventHandler extends TraceEventHandlerBase {

	private SystemModel system;

	public StatedumpInetSockEventHandler() {
		super();
		this.hooks.add(new TraceHook("lttng_statedump_inet_sock"));
		this.hooks.add(new TraceHook("lttng_statedump_end"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = ModelRegistry.getInstance().getOrCreateModel(reader, SystemModel.class);
		system.init(reader);
	}

	@Override
	public void handleComplete(TraceReader reader) {

	}

	public void handle_lttng_statedump_inet_sock(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		IntegerDefinition pid = (IntegerDefinition) def.get("_pid");
		//IntegerDefinition fd = (IntegerDefinition) def.get("_fd");
		IntegerDefinition sk = (IntegerDefinition) def.get("_sk");
		Inet4Sock sock = new Inet4Sock();
		sock.setSk(sk.getValue());
		Task task = system.getTask(pid.getValue());
		system.addInetSock(task, sock);
	}

	public void handle_lttng_statedump_end(TraceReader reader, EventDefinition event) {
		reader.cancel();
	}
}
