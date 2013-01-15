package org.lttng.studio.tests.graph;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.Subgraph;
import org.junit.Test;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.graph.ExecGraph;
import org.lttng.studio.model.graph.TaskGraphExtractor;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.AnalysisPhase;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.reader.handler.ITraceEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;
import org.lttng.studio.tests.basic.TestTraceset;
import org.lttng.studio.utils.GraphUtils;

public class TestTaskExecGraph {

	@Test
	public void testTaskExecGraph() throws IOException, TmfTraceException, InterruptedException {
		String name = "wk-cpm1-k";
		File out = new File("graph" + File.separator + name);
		out.mkdirs();
		File traceDir = TestTraceset.getKernelTrace(name);
		AnalyzerThread thread = new AnalyzerThread();
		CtfTmfTrace ctfTmfTrace = new CtfTmfTrace();
		ctfTmfTrace.initTrace(null, traceDir.getCanonicalPath(), CtfTmfEvent.class);

		Collection<ITraceEventHandler> phase1 = TraceEventHandlerFactory.makeStatedump();
		Collection<ITraceEventHandler> phase2 = TraceEventHandlerFactory.makeFull();

		thread.addTrace(ctfTmfTrace);
		thread.addPhase(new AnalysisPhase("phase1", phase1));
		thread.addPhase(new AnalysisPhase("phase2", phase2));
		thread.start();
		thread.join();

		ExecGraph graph = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, ExecGraph.class);
		SystemModel model = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
		Set<Task> task = model.getTaskByNameSuffix("wk-cpm1");

		assertTrue(task.size() > 0);
		assertTrue(graph.getGraph().vertexSet().size() > 0);

		for (Task t: task) {
			Subgraph<ExecVertex, ExecEdge, DirectedGraph<ExecVertex, ExecEdge>> subgraph = TaskGraphExtractor.getExecutionGraph(graph, graph.getStartVertexOf(t), graph.getEndVertexOf(t));
			String path = new File(out, t.getTid() + "-egraph.dot").getCanonicalPath();
			GraphUtils.saveGraphDefault(subgraph, path);
		}
	}

}
