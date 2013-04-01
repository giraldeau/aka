package org.lttng.studio.reader.handler;

import java.util.Collection;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.lttng.studio.model.kernel.Inet4Sock;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class TraceEventHandlerSock extends TraceEventHandlerBase {

	private SystemModel system;

	private ALog log;

	public TraceEventHandlerSock() {
		super();
		this.hooks.add(new TraceHook("inet_connect"));
		this.hooks.add(new TraceHook("inet_accept"));
		this.hooks.add(new TraceHook("inet_sock_clone"));
		this.hooks.add(new TraceHook("inet_sock_delete"));
		this.hooks.add(new TraceHook("inet_sock_create"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
		log = reader.getRegistry().getModel(IModelKeys.SHARED, ALog.class);
	}

	@Override
	public void handleComplete(TraceReader reader) {
		Collection<Inet4Sock> socks = system.getInetSocks();
		long now = reader.getCurrentTime();
		for (Inet4Sock sock: socks) {
			sock.setEndTime(now);
		}
	}

	public void defineInet4Sock(CtfTmfEvent event) {
		long sk = EventField.getLong(event, "sk");
		long saddr = EventField.getLong(event, "saddr");
		long daddr = EventField.getLong(event, "daddr");
		long sport = EventField.getLong(event, "sport");
		long dport = EventField.getLong(event, "dport");
		Task task = system.getTaskCpu(event.getCPU());
		Inet4Sock sock = system.getInetSock(task, sk);
		if (sock == null) {
			log.warning(String.format("missing inet_sock_create for sock 0x%x", sk));
			sock = new Inet4Sock();
			sock.setSk(sk);
			system.addInetSock(task, sock);
		}
		sock.setInet((int)saddr, (int)daddr, (int)sport, (int)dport);
		system.matchPeer(sock);
	}

	public void handle_inet_connect(TraceReader reader, CtfTmfEvent event) {
		defineInet4Sock(event);
	}

	public void handle_inet_accept(TraceReader reader, CtfTmfEvent event) {
		defineInet4Sock(event);
	}

	public void handle_inet_sock_clone(TraceReader reader, CtfTmfEvent event) {
		long osk = EventField.getLong(event, "osk");
		long nsk = EventField.getLong(event, "nsk");
		Inet4Sock oldSock = system.getInetSock(osk);
		Task owner = system.getInetSockTaskOwner(oldSock);
		if (oldSock == null) {
			log.warning("cloning unkown sock osk=" +
					Long.toHexString(osk) + " at " + event.getTimestamp().getValue());

			return;
		}
		Inet4Sock newSock = new Inet4Sock();
		newSock.setInet(oldSock);
		newSock.setSk(nsk);
		newSock.setStartTime(event.getTimestamp().getValue());
		system.addInetSock(owner, newSock);
	}

	public void handle_inet_sock_create(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		Task current = system.getTaskCpu(cpu);
		Inet4Sock sock = new Inet4Sock();
		long sk = EventField.getLong(event, "sk");
		sock.setSk(sk);
		sock.setStartTime(event.getTimestamp().getValue());
		system.addInetSock(current, sock);
	}

	public void handle_inet_sock_delete(TraceReader reader, CtfTmfEvent event) {
		// TODO: add state to Inet4Sock instead of delete
		long sk = EventField.getLong(event, "sk");
		system.removeInetSock(sk);
	}

}