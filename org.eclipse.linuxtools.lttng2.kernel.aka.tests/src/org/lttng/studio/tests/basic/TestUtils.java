package org.lttng.studio.tests.basic;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.lttng.studio.reader.AnalysisPhase;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.handler.ITraceEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;

public class TestUtils {

	public static AnalyzerThread setupAnalysis(String name) {
		File traceDir;
		try {
			traceDir = TestTraceset.getKernelTrace(name);
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		AnalyzerThread thread = new AnalyzerThread();
		CtfTmfTrace ctfTmfTrace = new CtfTmfTrace();
		try {
			ctfTmfTrace.initTrace(null, traceDir.getCanonicalPath(), CtfTmfEvent.class);
		} catch (TmfTraceException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		Collection<ITraceEventHandler> phase1 = TraceEventHandlerFactory.makeStatedump();
		Collection<ITraceEventHandler> phase2 = TraceEventHandlerFactory.makeInitialState();
		Collection<ITraceEventHandler> phase3 = TraceEventHandlerFactory.makeMain();

		thread.setTrace(ctfTmfTrace);
		thread.addPhase(new AnalysisPhase(1, "phase1", phase1));
		thread.addPhase(new AnalysisPhase(2, "phase2", phase2));
		thread.addPhase(new AnalysisPhase(3, "phase3", phase3));
		return thread;
	}

}
