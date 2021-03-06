package org.lttng.studio.tests.graph;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.lttng.studio.model.graph.CriticalPathStats;
import org.lttng.studio.model.graph.DepthFirstCriticalPathBackward;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecGraph;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.graph.Span;
import org.lttng.studio.reader.handler.ALog;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class TestGraphAnnotation {

	static HashMap<String, String> exp = new HashMap<String, String>();
	static {
		exp.put(BasicGraph.GRAPH_BASIC,		"A0-A1;A1-B1;B1-B2;B2-A2;A2-A3;");
		exp.put(BasicGraph.GRAPH_CONCAT,	"A0-A1;A1-B1;B1-B2;B2-A2;A2-A3;" +
											"A3-C3;C3-C4;C4-A4;A4-A5;");
		exp.put(BasicGraph.GRAPH_EMBEDED,	"A0-A1;A1-C1;C1-C4;C4-A4;A4-A5");
		exp.put(BasicGraph.GRAPH_INTERLEAVE,"A0-A1;A1-A2;A2-C2;C2-C4;C4-A4;A4-A5;");
		exp.put(BasicGraph.GRAPH_NESTED,	"A0-A1;A1-B1;B1-B2;B2-C2;C2-C3;C3-B3;B3-B4;B4-A4;A4-A5");
		exp.put(BasicGraph.GRAPH_OPEN1,		"A0-A1;A1-A2");
		exp.put(BasicGraph.GRAPH_OPEN2,		"B0-B1;B1-A1;A1-A2");
		exp.put(BasicGraph.GRAPH_GARBAGE1,	"A0-A1;A1-B1;B1-B2;B2-A2;A2-A3;");
		exp.put(BasicGraph.GRAPH_GARBAGE2,	"A0-A1;A1-B1;B1-B2;B2-A2;A2-A3;");
		exp.put(BasicGraph.GRAPH_GARBAGE3,	"A0-A1;A1-B1;B1-B2;B2-A2;A2-A3;A3-A4");
		exp.put(BasicGraph.GRAPH_DUPLICATE,	"A0-A1;A1-A2;A2-B2;B2-B3;B3-A3;A3-A4");
		exp.put(BasicGraph.GRAPH_BACKWARD1,	"B0-B1;B1-B2;B2-A2;A2-A3");
		exp.put(BasicGraph.GRAPH_BACKWARD2,	"C0-C1;C1-B1;B1-B2;B2-A2;A2-A3");
		exp.put(BasicGraph.GRAPH_BACKWARD3,	"B0-B1;B1-B2;B2-A2;A2-A3");
		exp.put(BasicGraph.GRAPH_MULTI1,	"A0-A1;A1-C1;C1-C2;C2-D2;D2-D3;D3-D4;D4-D5;D5-A5;A5-A6;A6-A7");
		exp.put(BasicGraph.GRAPH_SHELL,		"A0-A1;A1-B1;B1-B2;B2-C2;C2-C3;C3-C4;C4-C5;C5-C6;C6-C7;" +
											"C7-D7;D7-D8;D8-D9;D9-D10;D10-E10;E10-E11;E11-E12;" +
											"E12-E13;E13-E14;E14-E15;E15-B15;B15-B16;B16-B17;" +
											"B17-A17;A17-A18;");
	}
	// Total time according to alphabetical order actor (A, B, C, D, E)
	static HashMap<String, Integer[]> cp = new HashMap<String, Integer[]>();
	static {
		cp.put(BasicGraph.GRAPH_BASIC, 		new Integer[] { 2, 1, 0, 0, 0 });
		cp.put(BasicGraph.GRAPH_CONCAT, 	new Integer[] { 3, 1, 1, 0, 0});
		cp.put(BasicGraph.GRAPH_EMBEDED, 	new Integer[] { 2, 0, 3, 0, 0 });
		cp.put(BasicGraph.GRAPH_INTERLEAVE,	new Integer[] { 3, 0, 2, 0, 0 });
		cp.put(BasicGraph.GRAPH_NESTED, 	new Integer[] { 2, 2, 1, 0, 0 });
		cp.put(BasicGraph.GRAPH_OPEN1,		new Integer[] { 2, 0, 0, 0, 0 });
		cp.put(BasicGraph.GRAPH_OPEN2,		new Integer[] { 1, 1, 0, 0, 0 });
		cp.put(BasicGraph.GRAPH_GARBAGE1, 	new Integer[] { 2, 1, 0, 0, 0 });
		cp.put(BasicGraph.GRAPH_GARBAGE2, 	new Integer[] { 2, 1, 0, 0, 0 });
		cp.put(BasicGraph.GRAPH_GARBAGE3, 	new Integer[] { 3, 1, 0, 0, 0 });
		cp.put(BasicGraph.GRAPH_DUPLICATE, 	new Integer[] { 3, 1, 0, 0, 0 });
		cp.put(BasicGraph.GRAPH_BACKWARD1, 	new Integer[] { 1, 2, 0, 0, 0 });
		cp.put(BasicGraph.GRAPH_BACKWARD2, 	new Integer[] { 1, 1, 1, 0, 0 });
		cp.put(BasicGraph.GRAPH_BACKWARD3, 	new Integer[] { 1, 2, 0, 0, 0 });
		cp.put(BasicGraph.GRAPH_MULTI1, 	new Integer[] { 3, 0, 1, 3, 0 });
		cp.put(BasicGraph.GRAPH_SHELL,	 	new Integer[] { 2, 3, 5, 3, 5 });
	}

	static String[] actors = new String[] { "A", "B", "C", "D", "E" };
	boolean debugMode = false;

	@Before
	public void setup() {
		String var = System.getenv("DEBUG_MODE");
		if (var != null && var.length() > 0) {
			debugMode = true;
		}
	}

	@Test
	public void testGraphStatsAll() {
		for (String name: cp.keySet()) {
			testGraphStats(name);
		}
	}

	public void testGraphStats(String name) {
		ExecGraph graph = BasicGraph.makeGraphByName(name);
		ExecVertex start = BasicGraph.getVertexByName(graph, "A0");
		ExecVertex stop = graph.getEndVertexOf(start.getOwner());
		DepthFirstCriticalPathBackward annotate = new DepthFirstCriticalPathBackward(graph);
		List<ExecEdge> path = annotate.criticalPath(start, stop);
		Span root = CriticalPathStats.compile(graph, path);
		Multimap<Object, Span> ownerSpanIndex = CriticalPathStats.makeOwnerSpanIndex(root);
		if (debugMode) {
			System.out.println(name);
			System.out.println(ownerSpanIndex);
			System.out.println(CriticalPathStats.formatStats(ownerSpanIndex.values()));
			System.out.println(CriticalPathStats.formatSpanHierarchy(root));
		}
		Integer[] data = cp.get(name);
		for (int i = 0; i < data.length; i++) {
			ExecVertex v = BasicGraph.getVertexByPrefix(graph, actors[i]);
			if (debugMode)
				System.out.println("v=" + v + " data[i]=" + data[i]);
			if (data[i] == 0 && v == null)
				continue;
			Collection<Span> span = ownerSpanIndex.get(v.getOwner());
			if (data[i] == 0 && span.isEmpty()) {
				continue;
			}
			if (data[i] > 0 && span.isEmpty()) {
				System.err.println("span should not be null for " + v.getOwner());
				continue;
			}
			long sum = 0;
			for (Span s: span) {
				sum += s.getSelfTime();
			}
			if (debugMode)
				System.out.println(v.getOwner() + " " + sum + " == " + data[i]);
			if (!debugMode) {
				assertEquals((long)data[i], sum);
			}
		}
	}

	@Test
	public void testGraphAnnotateBackwardBasic() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_BASIC);
	}

	@Test
	public void testGraphAnnotateBackwardConcat() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_CONCAT);
	}

	@Test
	public void testGraphAnnotateBackwardEmbeded() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_EMBEDED);
	}

	@Test
	public void testGraphAnnotateBackwardInterleave() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_INTERLEAVE);
	}

	@Test
	public void testGraphAnnotateBackwardNested() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_NESTED);
	}

	@Test
	public void testGraphAnnotateBackwardOpen1() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_OPEN1);
	}

	@Test
	public void testGraphAnnotateBackwardOpen2() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_OPEN2);
	}

	@Test
	public void testGraphAnnotateBackwardGarbage1() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_GARBAGE1);
	}

	@Test
	public void testGraphAnnotateBackwardGarbage2() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_GARBAGE2);
	}

	@Test
	public void testGraphAnnotateBackwardGarbage3() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_GARBAGE3);
	}

	@Test
	public void testGraphAnnotateBackwardDuplicate() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_DUPLICATE);
	}

	@Test
	public void testGraphAnnotateBackwardBackward1() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_BACKWARD1);
	}

	@Test
	public void testGraphAnnotateBackwardBackward2() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_BACKWARD2);
	}

	@Test
	public void testGraphAnnotateBackwardBackward3() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_BACKWARD3);
	}

	@Test
	public void testGraphAnnotateBackwardMulti1() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_MULTI1);
	}

	@Test
	public void testGraphAnnotateBackwardShell() {
		testGraphAnnotateBackward(BasicGraph.GRAPH_SHELL);
	}

	public void testGraphAnnotateBackward(String curr) {
		ExecGraph graph = BasicGraph.makeGraphByName(curr);
		ExecVertex start = BasicGraph.getVertexByName(graph, "A0");
		ExecVertex stop = graph.getEndVertexOf(start.getOwner());
		ALog log = new ALog();
		log.setLevel(ALog.DEBUG);
		log.setPath("graph/tests/" + curr + "-backward.log");
		DepthFirstCriticalPathBackward annotate = new DepthFirstCriticalPathBackward(graph, log);
		List<ExecEdge> path = annotate.criticalPath(start, stop);
		List<ExecEdge> expRed = getExpectedRedEdges(graph, curr);
		checkPath(curr, expRed, path);
	}

	public void checkPath(String name, List<ExecEdge> expRed, List<ExecEdge> actRed) {
		SetView<ExecEdge> diff = Sets.symmetricDifference(new HashSet<ExecEdge>(expRed), new HashSet<ExecEdge>(actRed));
		if (diff.size() != 0) {
			System.out.println("FAILED " + name);
			System.out.println("Expected:");
			for (ExecEdge e: expRed)
				System.out.println(e);
			System.out.println("Actual:");
			for (ExecEdge e: actRed)
				System.out.println(e);
			System.out.println("Difference (size=" + diff.size() + "):");
			for (ExecEdge e: diff)
				System.out.println(e);
		}
		if (!debugMode)
			assertEquals(0, diff.size());
	}

	static List<ExecEdge> getExpectedRedEdges(ExecGraph graph, String name) {
		String s = exp.get(name);
		String[] splits = s.split(";");
		List<ExecEdge> list = new ArrayList<ExecEdge>();
		if (s.length() > 0) {
			for (String split: splits) {
				String[] endpoints = split.split("-");
				if (endpoints.length != 2) {
					throw new RuntimeException("Malformed expression " + name + " " + s);
				}
				ExecEdge e = BasicGraph.getEdgeByName(graph, endpoints[0], endpoints[1]);
				if (e == null)
					throw new RuntimeException("Expected set must not contains null edge " + Arrays.toString(endpoints));
				list.add(e);
			}
		}
		return list;
	}

	static HashSet<ExecEdge> getEdgesByType(HashMap<ExecEdge, Integer> map, Integer type) {
		HashSet<ExecEdge> set = new HashSet<ExecEdge>();
		for (Entry<ExecEdge, Integer> entry: map.entrySet()) {
			if (entry.getValue() == type) {
				set.add(entry.getKey());
			}
		}
		return set;
	}

}
