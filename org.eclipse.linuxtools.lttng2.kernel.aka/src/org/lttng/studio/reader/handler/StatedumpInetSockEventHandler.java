package org.lttng.studio.reader.handler;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.lttng.studio.model.kernel.Inet4Sock;
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
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
	}

	@Override
	public void handleComplete(TraceReader reader) {

	}

	public void handle_lttng_statedump_inet_sock(TraceReader reader, CtfTmfEvent event) {
		long pid = EventField.getLong(event, "pid");
		long sk = EventField.getLong(event, "sk");
		//long fd = EventField.getLong(event, "fd");
		Inet4Sock sock = new Inet4Sock();
		sock.setSk(sk);
		Task task = system.getTask(pid);
		system.addInetSock(task, sock);
	}

	public void handle_lttng_statedump_end(TraceReader reader, CtfTmfEvent event) {
		reader.cancel();
	}
}
