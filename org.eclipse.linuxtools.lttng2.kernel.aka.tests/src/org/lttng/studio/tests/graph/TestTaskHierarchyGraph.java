package org.lttng.studio.tests.graph;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.junit.Test;
import org.lttng.studio.model.graph.TaskHierarchyGraph;
import org.lttng.studio.reader.AnalysisPhase;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.reader.handler.ITraceEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;
import org.lttng.studio.tests.basic.TestTraceset;

public class TestTaskHierarchyGraph {

	@Test
	public void testBurnTaskHierarchy() throws IOException, TmfTraceException, InterruptedException {
		File traceDir = TestTraceset.getKernelTrace("burnP6-1x-1sec-k");
		AnalyzerThread thread = new AnalyzerThread();
		CtfTmfTrace ctfTmfTrace = new CtfTmfTrace();
		ctfTmfTrace.initTrace(null, traceDir.getCanonicalPath(), CtfTmfEvent.class);

		Collection<ITraceEventHandler> phase1 = TraceEventHandlerFactory.makeStatedump();
		Collection<ITraceEventHandler> phase2 = TraceEventHandlerFactory.makeFull();

		System.out.println(phase2);

		thread.addTrace(ctfTmfTrace);
		thread.addPhase(new AnalysisPhase("test", phase1));
		thread.addPhase(new AnalysisPhase("test", phase2));
		thread.start();
		thread.join();

		TaskHierarchyGraph model = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, TaskHierarchyGraph.class);
		assertTrue(model.getGraph().vertexSet().size() > 0);
	}


}
