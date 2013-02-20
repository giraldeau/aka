package org.lttng.studio.tests.graph;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.Subgraph;
import org.jgrapht.traverse.AbstractGraphIterator;
import org.junit.Test;
import org.lttng.studio.model.graph.ClosestFirstCriticalPathAnnotation;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecGraph;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.graph.ForwardClosestIterator;
import org.lttng.studio.model.graph.TaskGraphExtractor;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.handler.IModelKeys;
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

	private void saveGraph(ExecGraph graph, String name) throws IOException {
		File out = getGraphOutDir(name);
		File file = new File(out, "graph" + ".dot");
		FileWriter f = new FileWriter(file);
		f.write("digraph G {\n");
		HashSet<ExecVertex> set = new HashSet<ExecVertex>();
		for (ExecEdge edge: graph.getGraph().edgeSet()) {
			ExecVertex src = graph.getGraph().getEdgeSource(edge);
			ExecVertex dst = graph.getGraph().getEdgeTarget(edge);
			set.add(src);
			set.add(dst);
			f.write(String.format("%d -> %d [ label=\"%s\" ];\n", src.getId(), dst.getId(), edge.getType()));
		}
		for (ExecVertex vertex: set) {
			String str = vertex.getOwner().toString();
			if (vertex.getOwner() instanceof Task) {
				str = "" + ((Task)vertex.getOwner()).getTid();
			}
			f.write(String.format("%d [ label=\"[%d] %s\" ];\n", vertex.getId(), vertex.getId(), str));
		}
		f.write("}\n");
		f.flush();
		f.close();
	}

	private void saveGraphTasks(ExecGraph graph, Set<Task> task, String name) throws IOException {
		assertTrue(task.size() > 0);
		assertTrue(graph.getGraph().vertexSet().size() > 0);
		File out = getGraphOutDir(name);
		for (Task t: task) {
			Subgraph<ExecVertex, ExecEdge, DirectedGraph<ExecVertex, ExecEdge>> subgraph =
					TaskGraphExtractor.getExecutionGraph(graph, graph.getStartVertexOf(t), graph.getEndVertexOf(t));
			String path = new File(out, t.getTid() + "-egraph").getCanonicalPath();
			GraphUtils.saveGraphDefault(subgraph, path);
		}
	}


	public void saveEdges(ExecGraph graph, HashMap<ExecEdge, Integer> map, Task task, String string) throws IOException {
		File file = new File(getGraphOutDir(string), task.getTid() + ".dot");
		FileWriter f = new FileWriter(file);
		f.write("digraph G {\n");
		HashSet<ExecVertex> set = new HashSet<ExecVertex>();
		for (ExecEdge edge: map.keySet()) {
			ExecVertex src = graph.getGraph().getEdgeSource(edge);
			ExecVertex dst = graph.getGraph().getEdgeTarget(edge);
			set.add(src);
			set.add(dst);
			f.write(String.format("%d -> %d [ label=\"%d,%s\" ];\n",src.getId(), dst.getId(), map.get(edge), edge.getType()));
		}
		for (ExecVertex vertex: set) {
			String str = vertex.getOwner().toString();
			if (vertex.getOwner() instanceof Task) {
				str = "" + ((Task)vertex.getOwner()).getTid();
			}
			f.write(String.format("%d [ label=\"[%d] %s\" ];\n", vertex.getId(), vertex.getId(), str));
		}
		f.write("}\n");
		f.flush();
		f.close();
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

	@Test
	public void testCPMpath() throws TmfTraceException, IOException, InterruptedException {
		String[][] s = new String[][] {
					{ "wk-cpm1-k", "wk-cpm1" },
					{ "wk-cpm2-k", "wk-cpm2" },
					{ "burnP6-16x-1sec-k", "burnP6-16x-1sec" }
				};
		for (int i = 0; i < s.length; i++) {
			computeCriticalPath(s[i][0], s[i][1]);
		}
	}

	@Test
	public void testCPMOne() throws TmfTraceException, IOException, InterruptedException {
		computeCriticalPath("burnP6-16x-1sec-k", "burnP6-16x-1sec");
	}

	private void computeCriticalPath(String name, String comm) throws TmfTraceException, IOException, InterruptedException {
		AnalyzerThread thread = new AnalyzerThread();
		thread.setTrace(TestTraceset.getKernelTrace(name));
		thread.addAllPhases(TraceEventHandlerFactory.makeStandardAnalysis());
		thread.start();
		thread.join();

		ExecGraph graph = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, ExecGraph.class);
		SystemModel model = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
		saveGraph(graph, comm);
		Set<Task> set = model.getTaskByNameSuffix(comm);
		for (Task task: set) {
			ExecVertex head = graph.getStartVertexOf(task);
			ClosestFirstCriticalPathAnnotation traversal = new ClosestFirstCriticalPathAnnotation(graph);
			//traversal.setDebug(true);
			AbstractGraphIterator<ExecVertex, ExecEdge> iter =
					new ForwardClosestIterator<ExecVertex, ExecEdge>(graph.getGraph(), head);
			iter.addTraversalListener(traversal);
			while (iter.hasNext() && !traversal.isDone()) {
				iter.next();
			}
			HashMap<ExecEdge, Integer> map = traversal.getEdgeState();
			System.out.println(map);
			saveEdges(graph, map, task, name + "-edges");
		}
	}

}
