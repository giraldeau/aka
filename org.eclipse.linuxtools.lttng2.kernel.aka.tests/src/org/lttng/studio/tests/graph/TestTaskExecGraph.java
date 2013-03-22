package org.lttng.studio.tests.graph;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.Subgraph;
import org.junit.Test;
import org.lttng.studio.model.graph.CriticalPathStats;
import org.lttng.studio.model.graph.DepthFirstCriticalPathBackward;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecGraph;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.graph.Span;
import org.lttng.studio.model.graph.TaskGraphExtractor;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.handler.ALog;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;
import org.lttng.studio.tests.basic.TestTraceset;
import org.lttng.studio.tests.basic.TestUtils;
import org.lttng.studio.utils.AnalysisFilter;
import org.lttng.studio.utils.GraphUtils;

import com.google.common.collect.ArrayListMultimap;

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


	public void saveEdges(ExecGraph graph, List<ExecEdge> path, Task task, String string) throws IOException {
		File file = new File(getGraphOutDir(string), task.getTid() + ".dot");
		FileWriter f = new FileWriter(file);
		ArrayListMultimap<Object, ExecEdge> edgeMap = ArrayListMultimap.create();
		for (ExecEdge edge: path) {
			ExecVertex src = graph.getGraph().getEdgeSource(edge);
			ExecVertex dst = graph.getGraph().getEdgeTarget(edge);
			if (src.getOwner() == dst.getOwner())
				edgeMap.put(dst.getOwner(), edge);
		}
		String vertexFmt = "    %d [ label=\"[%d]\" ];\n";
		f.write("digraph G {\n");
		f.write("  rankdir=LR;\n");
		int i = 0;
		HashSet<ExecVertex> seenVertex = new HashSet<ExecVertex>();
		HashSet<ExecEdge> seenEdge = new HashSet<ExecEdge>();
		for (Object actor: edgeMap.keySet()) {
			f.write(String.format("  subgraph \"cluster_%d\" {\n", i));
			f.write("    rankdir=LR\n");
			f.write(String.format("    title%d [ label=\"%s\", shape=plaintext ];\n", i, actor));
			for (ExecEdge edge: edgeMap.get(actor)) {
				ExecVertex src = graph.getGraph().getEdgeSource(edge);
				ExecVertex dst = graph.getGraph().getEdgeTarget(edge);
				if (!seenVertex.contains(src)) {
					f.write(String.format(vertexFmt, src.getId(), src.getId()));
					seenVertex.add(src);
				}
				if (!seenVertex.contains(dst)) {
					f.write(String.format(vertexFmt, dst.getId(), dst.getId()));
					seenVertex.add(dst);
				}
				f.write(String.format("    %d -> %d [ label=\"%s\" ];\n", src.getId(), dst.getId(), edge.getType()));
				seenEdge.add(edge);
			}
			i++;
			f.write("}\n");
		}
		for (ExecEdge edge: path) {
			if (!seenEdge.contains(edge)) {
				ExecVertex src = graph.getGraph().getEdgeSource(edge);
				ExecVertex dst = graph.getGraph().getEdgeTarget(edge);
				f.write(String.format("    %d -> %d [ label=\"%s\" ];\n", src.getId(), dst.getId(), edge.getType()));
			}
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
					{ "burnP6-16x-1sec-k", "burnP6-16x-1sec" },
					{ "wk-imbalance-k", "wk-imbalance" },
					{ "wk-mutex-k", "wk-mutex" },
					{ "wk-pipeline-k", "wk-pipeline" },
					{ "wk-inception-3x-100ms-k", "wk-inception" },
					{ "netcat-tcp-k", "netcat-tcp" },
					{ "wk-pipette-cons-k", "wk-pipette" },
					{ "wk-pipette-prod-k", "wk-pipette" },
				};
		for (int i = 0; i < s.length; i++) {
			computeCriticalPathTest(s[i][0], s[i][1]);
		}
	}

	@Test
	public void testCPMOne() throws TmfTraceException, IOException, InterruptedException {
		computeCriticalPathTest("wk-pipette-cons-k", "wk-pipette");
	}

	private void computeCriticalPathTest(String name, String comm) throws TmfTraceException, IOException, InterruptedException {
		AnalyzerThread thread = new AnalyzerThread();
		thread.setTrace(TestTraceset.getKernelTrace(name));
		ALog log = thread.getReader().getRegistry().getOrCreateModel(IModelKeys.SHARED, ALog.class);
		System.out.println("PROCESSING " + name);
		File outDir = getGraphOutDir(name);
		log.setPath(new File(outDir, name + ".log").getCanonicalPath());
		log.setLevel(ALog.DEBUG);
		thread.addAllPhases(TraceEventHandlerFactory.makeStandardAnalysis());
		thread.start();
		thread.join();

		ExecGraph graph = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, ExecGraph.class);
		SystemModel model = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
		saveGraph(graph, name);
		Set<Task> set = model.getTaskByNameSuffix(comm);
		for (Task task: set) {
			log.debug("COMPUTE_CRITICAL_PATH " + task);
			ExecVertex start = graph.getStartVertexOf(task);
			ExecVertex stop = graph.getEndVertexOf(task);
			DepthFirstCriticalPathBackward annotate = new DepthFirstCriticalPathBackward(graph, log);
			List<ExecEdge> path = annotate.criticalPath(start, stop);
			checkEdgesDisjoint(graph, path);
			saveEdges(graph, path, task, name);
			saveStats(graph, path, name, "" + task.getTid());
		}
	}

	private void checkEdgesDisjoint(ExecGraph graph, List<ExecEdge> path) {
		for (int i = 0; i < path.size() - 1; i++) {
			ExecVertex e1 = graph.getGraph().getEdgeTarget(path.get(i));
			ExecVertex e2 = graph.getGraph().getEdgeSource(path.get(i + 1));
			assertTrue(e1.getTimestamp() <= e2.getTimestamp());
		}
	}

	private void saveStats(ExecGraph graph, List<ExecEdge> path, String name, String tid) throws IOException {
		Span root = CriticalPathStats.compile(graph, path);
		String formatStats = CriticalPathStats.formatStats(root);
		String formatSpan = CriticalPathStats.formatSpanHierarchy(root);
		File graphOutDir = getGraphOutDir(name);
		File fout = new File(graphOutDir, name + "-" + tid + ".stats");
		FileWriter writer = new FileWriter(fout);
		writer.write(name + " " + "tid=" + tid + " " + "head=" + tid + "\n");
		writer.write(formatStats);
		writer.write(formatSpan);
		writer.flush();
		writer.close();
	}

}
