package org.lttng.studio.tests.graph;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Test;
import org.lttng.studio.model.graph.CriticalPathStats;
import org.lttng.studio.model.graph.DepthFirstCriticalPathAnnotation;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecGraph;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.graph.Span;

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
		exp.put(BasicGraph.GRAPH_OPEN2,		"B0-B1;A1-A2");
		exp.put(BasicGraph.GRAPH_GARBAGE1,	"A0-A1;A1-B1;B1-B2;B2-A2;A2-A3;");
		exp.put(BasicGraph.GRAPH_GARBAGE2,	"A0-A1;A1-B1;B1-B2;B2-A2;A2-A3;");
		exp.put(BasicGraph.GRAPH_GARBAGE3,	"A0-A1;A1-B1;B1-B2;B2-A2;A2-A3;A3-A4");
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
		cp.put(BasicGraph.GRAPH_GARBAGE1, 	new Integer[] { 2, 1, 0, 0, 0 });
		cp.put(BasicGraph.GRAPH_GARBAGE2, 	new Integer[] { 2, 1, 0, 0, 0 });
		cp.put(BasicGraph.GRAPH_GARBAGE3, 	new Integer[] { 3, 1, 0, 0, 0 });
		cp.put(BasicGraph.GRAPH_SHELL,	 	new Integer[] { 2, 3, 5, 3, 5 });
	}

	static String[] actors = new String[] { "A", "B", "C", "D", "E" };

	@Test
	public void testGraphStatsAll() {
		for (String name: cp.keySet()) {
			testGraphStats(name);
		}
	}

	@Test
	public void testGraphStatsOne() {
		testGraphStats(BasicGraph.GRAPH_BASIC);
	}

	public void testGraphStats(String name) {
		ExecGraph graph = BasicGraph.makeGraphByName(name);
		ExecVertex start = BasicGraph.getVertexByName(graph, "A0");
		Span root = CriticalPathStats.compile(graph, start);
		HashMap<Object, Span> ownerSpanIndex = CriticalPathStats.makeOwnerSpanIndex(root);
		//System.out.println(ownerSpanIndex);
		//String out = CriticalPathStats.formatStats(ownerSpanIndex.values());
		//System.out.println(name);
		//System.out.println(out);
		Integer[] data = cp.get(name);
		for (int i = 0; i < data.length; i++) {
			ExecVertex v = BasicGraph.getVertexByPrefix(graph, actors[i]);
			if (data[i] == 0 && v == null)
				continue;
			Span span = ownerSpanIndex.get(v.getOwner());
			if (data[i] == 0 && span == null)
				continue;
			//System.out.println(v.getOwner() + " " + span.getTotal() + " == " + data[i]);
			assertEquals((long)data[i], span.getSelfTime());
		}
	}

	@Test
	public void testGraphAnnotateClosestFirstAll() {
		for (String name: exp.keySet()) {
			System.out.println("processing " + name);
			testGraphAnnotateClosestFirst(name);
		}
	}

	@Test
	public void testOne() {
		testGraphAnnotateClosestFirst(BasicGraph.GRAPH_BASIC);
	}

	public void testGraphAnnotateClosestFirst(String curr) {
		ExecGraph graph = BasicGraph.makeGraphByName(curr);
		ExecVertex head = BasicGraph.getVertexByName(graph, "A0");
		List<ExecEdge> path = CriticalPathStats.computeCriticalPath(graph, head);
		HashSet<ExecEdge> expRed = getExpectedRedEdges(graph, curr);
		HashSet<ExecEdge> actRed = new HashSet<ExecEdge>(path);
		checkPath(curr, expRed, actRed);
	}

	@Test
	public void testGraphDepthFirstAll() {
		for (String name: exp.keySet()) {
			testGraphAnnotateDepthFirst(name);
		}
	}

	@Test
	public void testGraphDepthFirstOne() {
		testGraphAnnotateDepthFirst(BasicGraph.GRAPH_GARBAGE2);
	}

	public void testGraphAnnotateDepthFirst(String curr) {
		ExecGraph graph = BasicGraph.makeGraphByName(curr);
		ExecVertex start = BasicGraph.getVertexByName(graph, "A0");
		HashMap<ExecEdge, Integer> map = DepthFirstCriticalPathAnnotation.computeCriticalPath(graph, start);
		HashSet<ExecEdge> expRed = getExpectedRedEdges(graph, curr);
		HashSet<ExecEdge> actRed = getEdgesByType(map, ExecEdge.RED);
		checkPath(curr, expRed, actRed);
	}

	public void checkPath(String name, HashSet<ExecEdge> expRed, HashSet<ExecEdge> actRed) {
		SetView<ExecEdge> diff = Sets.symmetricDifference(expRed, actRed);
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
		assertEquals(0, diff.size());
	}

	static HashSet<ExecEdge> getExpectedRedEdges(ExecGraph graph, String name) {
		String s = exp.get(name);
		String[] splits = s.split(";");
		HashSet<ExecEdge> set = new HashSet<ExecEdge>();
		for (String split: splits) {
			String[] endpoints = split.split("-");
			ExecEdge e = BasicGraph.getEdgeByName(graph, endpoints[0], endpoints[1]);
			if (e == null)
				throw new RuntimeException("Expected set must not contains null edge " + Arrays.toString(endpoints));
			set.add(e);
		}
		return set;
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
