package org.lttng.studio.reader.handler;

import java.util.ArrayList;
import java.util.Collection;


public class TraceEventHandlerFactory {

	public static Collection<ITraceEventHandler> makeEmpty() {
		return new ArrayList<ITraceEventHandler>();
	}

	public static Collection<ITraceEventHandler> makeBasic() {
		ArrayList<ITraceEventHandler> list = new ArrayList<ITraceEventHandler>();
		list.add(new TraceEventHandlerCounter());
		return list;
	}

	public static Collection<ITraceEventHandler> makeStatedump() {
		ArrayList<ITraceEventHandler> list = new ArrayList<ITraceEventHandler>();
		list.add(new StatedumpEventHandler());
		list.add(new StatedumpInetSockEventHandler());
		return list;
	}

	public static Collection<ITraceEventHandler> makeFull() {
		ArrayList<ITraceEventHandler> list = new ArrayList<ITraceEventHandler>();
		list.add(new TraceEventHandlerSched());
		list.add(new TraceEventHandlerTaskHierarchy());
		list.add(new TraceEventHandlerFD());
		list.add(new TraceEventHandlerSock());
		list.add(new TraceEventHandlerNetPacket());
		list.add(new TraceEventHandlerBlocking());
		list.add(new TraceEventHandlerTaskExecutionGraph());
		list.add(new TraceEventHandlerCounter());
		return list;
	}

}
