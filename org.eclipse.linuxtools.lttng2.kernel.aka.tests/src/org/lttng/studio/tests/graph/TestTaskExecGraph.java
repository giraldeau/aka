package org.lttng.studio.tests.graph;

import static org.junit.Assert.*;
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
import org.lttng.studio.tests.basic.TestUtils;
import org.lttng.studio.utils.AnalysisFilter;
import org.lttng.studio.utils.GraphUtils;

public class TestTaskExecGraph {

	public File getGraphOutDir(String name) {
		File out = new File("graph" + File.separator + name);
		out.mkdirs();
		return out;
	}

	private void saveGraphTasks(ExecGraph graph, Set<Task> task, String name) throws IOException {
		assertTrue(task.size() > 0);
		assertTrue(graph.getGraph().vertexSet().size() > 0);
		File out = getGraphOutDir(name);
		for (Task t: task) {
			Subgraph<ExecVertex, ExecEdge, DirectedGraph<ExecVertex, ExecEdge>> subgraph = 
					TaskGraphExtractor.getExecutionGraph(graph, graph.getStartVertexOf(t), graph.getEndVertexOf(t));
			String path = new File(out, t.getTid() + "-egraph.dot").getCanonicalPath();
			GraphUtils.saveGraphDefault(subgraph, path);
		}		
	}
	
	@Test
	public void testSleep1() throws InterruptedException, IOException {
		String name = "sleep-1x-1sec-k";
		AnalyzerThread thread = TestUtils.setupAnalysis(name);
		assertNotNull(thread);
		AnalysisFilter filter = thread.getReader().getRegistry().getOrCreateModel(IModelKeys.SHARED, AnalysisFilter.class);
		filter.addCommand(".*sleep-1x-1sec");
		filter.setFollowChild(true);
		thread.start();
		thread.join();

		ExecGraph graph = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, ExecGraph.class);
		SystemModel model = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
		Set<Task> task = model.getTaskByNameSuffix("sleep-1x-1sec");

		saveGraphTasks(graph, task, name);
	}
	
	@Test
	public void testCPM1() throws InterruptedException, IOException {
		String name = "wk-cpm1-k";
		AnalyzerThread thread = TestUtils.setupAnalysis(name);
		assertNotNull(thread);
		AnalysisFilter filter = thread.getReader().getRegistry().getOrCreateModel(IModelKeys.SHARED, AnalysisFilter.class);
		filter.addCommand(".*wk-cpm1");
		thread.start();
		thread.join();

		ExecGraph graph = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, ExecGraph.class);
		SystemModel model = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
		Set<Task> task = model.getTaskByNameSuffix("wk-cpm1");

		saveGraphTasks(graph, task, name);
	}

}
