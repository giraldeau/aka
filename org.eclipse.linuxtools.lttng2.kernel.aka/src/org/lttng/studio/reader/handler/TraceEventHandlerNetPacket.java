package org.lttng.studio.reader.handler;

import java.util.HashMap;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDefinition;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class TraceEventHandlerNetPacket extends TraceEventHandlerBase {

	enum Type { SEND, RECV };

	class Event {
		public long ts;
		public long sk;
		public int seq;
		public Type type;
		public Event(long ts, long sk, int seq, Type type) {
			this.ts = ts;
			this.sk = sk;
			this.seq = seq;
			this.type = type;
		}
	};

	// FIXME: seq is not unique, should add port?

	HashMap<Integer, Event> match; // (seq, ev)
	private SystemModel system;

	public TraceEventHandlerNetPacket() {
		super();
		this.hooks.add(new TraceHook("inet_sock_local_in"));
		this.hooks.add(new TraceHook("inet_sock_local_out"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = ModelRegistry.getInstance().getOrCreateModel(reader, SystemModel.class);
		system.init(reader);
		match = new HashMap<Integer, TraceEventHandlerNetPacket.Event>();
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

	public Event makeEvent(EventDefinition event, Type type) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		long sk = ((IntegerDefinition) def.get("_sk")).getValue();
		int seq = (int) ((IntegerDefinition) def.get("_seq")).getValue();
		return new Event(event.getTimestamp(), sk, seq, type);
	}
	public void handle_inet_sock_local_in(TraceReader reader, EventDefinition event) {
		Event recv = makeEvent(event, Type.RECV);
		if (recv.sk == 0)
			return;

		Event send = match.remove(recv.seq);
		if (send == null)
			return;
		//System.out.println("RECV " + recv.sk + " " + recv.seq);

		assert(send.seq == recv.seq);
		assert(send.type == Type.SEND);
		assert(recv.type == Type.RECV);
		/*
		Actor sender = getOrCreateActor(send.sk);
		Actor receiver = getOrCreateActor(recv.sk);
		msgs.add(new Message(sender, send.ts, receiver, recv.ts));
		*/
	}

	public void handle_inet_sock_local_out(TraceReader reader, EventDefinition event) {
		Event send = makeEvent(event, Type.SEND);
		//System.out.println("SEND " + send.sk + " " + send.seq);
		match.put(send.seq, send);
	}

}
