package org.lttng.studio.reader.handler;

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDefinition;
import org.eclipse.linuxtools.tmf.ui.views.histogram.HistogramUtils;
import org.lttng.studio.model.kernel.Inet4Sock;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TimeKeeper;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class TraceEventHandlerSock extends TraceEventHandlerBase {

	private SystemModel system;

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
	}

	@Override
	public void handleComplete(TraceReader reader) {
		Collection<Inet4Sock> socks = system.getInetSocks();
		TimeKeeper time = TimeKeeper.getInstance();
		for (Inet4Sock sock: socks) {
			sock.setEndTime(time.getCurrentTime());
		}
	}

	public void defineInet4Sock(EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		IntegerDefinition sk = (IntegerDefinition) def.get("_sk");
		IntegerDefinition saddr = (IntegerDefinition) def.get("_saddr");
		IntegerDefinition daddr = (IntegerDefinition) def.get("_daddr");
		IntegerDefinition sport = (IntegerDefinition) def.get("_sport");
		IntegerDefinition dport = (IntegerDefinition) def.get("_dport");
		Task task = system.getTaskCpu(event.getCPU());
		Inet4Sock sock = system.getInetSock(task, sk.getValue());
		if (sock == null) {
			System.out.println("Huston, we missed inet_sock_create " + sk.getValue());
			sock = new Inet4Sock();
			sock.setSk(sk.getValue());
			system.addInetSock(task, sock);
		}
		sock.setInet((int)saddr.getValue(), (int)daddr.getValue(),
				(int)sport.getValue(), (int)dport.getValue());
		system.matchPeer(sock);
	}

	public void handle_inet_connect(TraceReader reader, EventDefinition event) {
		defineInet4Sock(event);
	}

	public void handle_inet_accept(TraceReader reader, EventDefinition event) {
		defineInet4Sock(event);
	}

	public void handle_inet_sock_clone(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		IntegerDefinition osk = (IntegerDefinition) def.get("_osk");
		IntegerDefinition nsk = (IntegerDefinition) def.get("_nsk");
		Inet4Sock oldSock = system.getInetSock(osk.getValue());
		Task owner = system.getInetSockTaskOwner(oldSock);
		if (oldSock == null) {
			long time = TraceReader.clockTime(event);
			String nano = HistogramUtils.nanosecondsToString(time);
			System.err.println("WARNING: cloning unkown sock osk=" +
					Long.toHexString(osk.getValue()) + " at " + nano);

			return;
		}
		Inet4Sock newSock = new Inet4Sock();
		newSock.setInet(oldSock);
		newSock.setSk(nsk.getValue());
		newSock.setStartTime(event.getTimestamp());
		system.addInetSock(owner, newSock);
	}

	public void handle_inet_sock_create(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		int cpu = event.getCPU();
		Task current = system.getTaskCpu(cpu);
		Inet4Sock sock = new Inet4Sock();
		IntegerDefinition sk = (IntegerDefinition) def.get("_sk");
		sock.setSk(sk.getValue());
		sock.setStartTime(event.getTimestamp());
		system.addInetSock(current, sock);
	}

	public void handle_inet_sock_delete(TraceReader reader, EventDefinition event) {
		// TODO: add state to Inet4Sock instead of delete
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		IntegerDefinition sk = (IntegerDefinition) def.get("_sk");
		system.removeInetSock(sk.getValue());
	}

}