package org.lttng.studio.tests.graph;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.jgrapht.traverse.AbstractGraphIterator;
import org.junit.Test;
import org.lttng.studio.model.graph.CriticalPathAnnotation;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecGraph;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.graph.ForwardClosestIterator;

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
	}
	
	@Test
	public void testGraphAnnotateAll() {
		for (String name: exp.keySet()) {
			testGraphAnnotate(name);
		}
	}
	
	public void testGraphAnnotate(String curr) {
		ExecGraph graph = BasicGraph.makeGraphByName(curr);
		ExecVertex base = BasicGraph.getVertexByName(graph, "A0");
		CriticalPathAnnotation traversal = new CriticalPathAnnotation(graph);
		ExecVertex head = graph.getStartVertexOf(base.getOwner());
		AbstractGraphIterator<ExecVertex, ExecEdge> iter = 
				new ForwardClosestIterator<ExecVertex, ExecEdge>(graph.getGraph(), head);
		iter.addTraversalListener(traversal);
		while (iter.hasNext())
			iter.next();
		HashMap<ExecEdge, Integer> map = traversal.getEdgeState();
		HashSet<ExecEdge> expRed = getExpectedRedEdges(graph, curr);
		HashSet<ExecEdge> actRed = getEdgesByType(map, CriticalPathAnnotation.RED);
		
		SetView<ExecEdge> diff = Sets.symmetricDifference(expRed, actRed);
		if (diff.size() != 0) {
			System.out.println("FAILED " + curr);
			System.out.println("Expected:");
			System.out.println(expRed);
			System.out.println("Actual:");
			System.out.println(actRed);	
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
