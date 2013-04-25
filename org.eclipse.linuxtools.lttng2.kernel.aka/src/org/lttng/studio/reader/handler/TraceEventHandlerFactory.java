package org.lttng.studio.reader.handler;

import java.util.ArrayList;
import java.util.Collection;

import org.lttng.studio.reader.AnalysisPhase;


public class TraceEventHandlerFactory {

	public static Collection<ITraceEventHandler> makeEmpty() {
		return new ArrayList<ITraceEventHandler>();
	}

	public static Collection<ITraceEventHandler> makeBasic() {
		ArrayList<ITraceEventHandler> list = new ArrayList<ITraceEventHandler>();
		list.add(new TraceEventHandlerInvariant());
		list.add(new TraceEventHandlerCounter());
		return list;
	}

	public static Collection<ITraceEventHandler> makeStatedump() {
		ArrayList<ITraceEventHandler> list = new ArrayList<ITraceEventHandler>();
		list.add(new TraceEventHandlerInvariant());
		list.add(new StatedumpEventHandler());
		list.add(new StatedumpInetSockEventHandler());
		return list;
	}

	public static Collection<ITraceEventHandler> makeInitialState() {
		ArrayList<ITraceEventHandler> list = new ArrayList<ITraceEventHandler>();
		list.add(new TraceEventHandlerInvariant());
		list.add(new TraceEventHandlerSchedInit());
		return list;
	}

	public static Collection<ITraceEventHandler> makeMain() {
		ArrayList<ITraceEventHandler> list = new ArrayList<ITraceEventHandler>();
		list.add(new TraceEventHandlerInvariant());
		list.add(new TraceEventHandlerSched());
		list.add(new TraceEventHandlerTaskHierarchy());
		list.add(new TraceEventHandlerHRTimer());
		list.add(new TraceEventHandlerFD());
		list.add(new TraceEventHandlerSock());
		list.add(new TraceEventHandlerNetPacket());
		list.add(new TraceEventHandlerBlocking());
		list.add(new TraceEventHandlerExecutionGraphLegacy());
		list.add(new TraceEventHandlerExecutionGraph());
		list.add(new TraceEventHandlerCounter());
		return list;
	}

	public static Collection<AnalysisPhase> makeStandardAnalysis() {
		ArrayList<AnalysisPhase> list = new ArrayList<AnalysisPhase>();
		list.add(new AnalysisPhase(1, "statedump", makeStatedump()));
		list.add(new AnalysisPhase(2, "initial state", makeInitialState()));
		list.add(new AnalysisPhase(3, "main analysis", makeMain()));
		return list;
	}

	public static Collection<AnalysisPhase> makeStandardAnalysisDebug() {
		ArrayList<AnalysisPhase> list = new ArrayList<AnalysisPhase>();
		list.add(new AnalysisPhase(1, "statedump", makeStatedump()));
		list.add(new AnalysisPhase(2, "initial state", makeInitialState()));
		Collection<ITraceEventHandler> main = makeMain();
		main.add(new TraceEventHandlerDebug());
		list.add(new AnalysisPhase(3, "main analysis", main));
		return list;
	}

}
