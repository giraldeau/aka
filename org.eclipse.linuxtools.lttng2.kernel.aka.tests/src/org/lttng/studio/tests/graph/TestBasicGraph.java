package org.lttng.studio.tests.graph;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.traverse.AbstractGraphIterator;
import org.junit.Test;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecGraph;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.graph.ForwardClosestIterator;
import org.lttng.studio.model.graph.ReverseClosestIterator;

import com.google.common.collect.ArrayListMultimap;

public class TestBasicGraph {

	@Test
	public void testReverseClosestTraversal() {
		final ExecGraph graph = BasicGraph.makeLengthUnequal();

		// retrieve the base object
		Object base = getBaseObject(graph);

		final StringBuilder str = new StringBuilder();
		ExecVertex tail = graph.getEndVertexOf(base);
		AbstractGraphIterator<ExecVertex, ExecEdge> iter = 
				new ReverseClosestIterator<ExecVertex, ExecEdge>(graph.getGraph(), tail);
		iter.addTraversalListener(new TraversalListenerAdapter<ExecVertex, ExecEdge>() {
			@Override
			public void vertexTraversed(VertexTraversalEvent<ExecVertex> item) {
				ExecVertex v = item.getVertex();
				str.append("" + v.getOwner() + v.getTimestamp());
			}
		});

		while (iter.hasNext())
			iter.next();
		
		assertTrue(str.toString().matches("A3B3B2B1A1"));
	}

	@Test
	public void testForwardClosestTraversal() {
		HashMap<String, String> exp = new HashMap<String, String>();
		String any = "(([A-Z][0-9]+) )*"; 
		exp.put(BasicGraph.GRAPH_BASIC, 		"A0 A1 B1 B2 A2 A3 ");
		exp.put(BasicGraph.GRAPH_CONCAT, 		"A0 A1 B1 B2 A2 A3 C3 C4 A4 A5 ");
		exp.put(BasicGraph.GRAPH_EMBEDED, 		"A0 A1 C1 A2 B2 B3 A3 C4 A4 A5 ");
		exp.put(BasicGraph.GRAPH_INTERLEAVE, 	"A0 A1 B1 A2 C2 B3 A3 C4 A4 A5 ");
		exp.put(BasicGraph.GRAPH_NESTED,		"A0 A1 B1 B2 C2 C3 B3 B4 A4 A5 ");
		exp.put(BasicGraph.GRAPH_OPEN1, 		"A0 A1 B1 ((A2|B2) ){2}");
		exp.put(BasicGraph.GRAPH_OPEN2, 		"A0 A1 A2 ");
		exp.put(BasicGraph.GRAPH_SHELL, 		any + "A1 " + any + "B1 " +
												any + "B2 " + any + "C2 " +
												any + "B3 " + any + "D3 " +
												any + "B5 " + any + "E5 " +
												any + "C7 " + any + "D7 " +
												any + "C8 " + any + "B8 " +
												any + "D10 " + any + "E10 " +
												any + "D11 " + any + "B11 " +
												any + "E13 " + any + "A13 " +
												any + "E15 " + any + "B15 " +
												any + "B17 " + any + "A17 " +
												any);
		// check that regex matches
		Set<String> graphName = BasicGraph.getGraphName();
		for (String name: graphName) {
			ExecGraph graph = BasicGraph.makeGraphByName(name);
			String str = getForwardClosestTraversalString(graph);
			//boolean matches = str.toString().matches(exp.get(name));
			//System.out.println(String.format("%20s %b %s", name, matches, str));
			assertTrue(str.toString().matches(exp.get(name)));
		}
	}
	
	private String getForwardClosestTraversalString(ExecGraph graph) {
		// retrieve the base object
		Object base = getBaseObject(graph);

		// check that timestamps increases
		// check that every node is encountered only once
		final StringBuilder str = new StringBuilder();
		ExecVertex tail = graph.getStartVertexOf(base);
		AbstractGraphIterator<ExecVertex, ExecEdge> iter = 
				new ForwardClosestIterator<ExecVertex, ExecEdge>(graph.getGraph(), tail);
		iter.addTraversalListener(new TraversalListenerAdapter<ExecVertex, ExecEdge>() {
			long time = 0;
			HashSet<ExecVertex> seen = new HashSet<ExecVertex>();
			@Override
			public void vertexTraversed(VertexTraversalEvent<ExecVertex> item) {
				ExecVertex v = item.getVertex();
				assertTrue(!seen.contains(v));
				assertTrue(time <= v.getTimestamp());
				str.append("" + v.getOwner() + v.getTimestamp() + " ");
				time = v.getTimestamp();
				seen.add(v);
			}
		});

		while (iter.hasNext())
			iter.next();
		
		return str.toString();
	}
	
	public Object getBaseObject(ExecGraph graph) {
		Object base = null;
		ArrayListMultimap<Object, ExecVertex> vertexMap = graph.getVertexMap();
		for (Object o : vertexMap.keySet()) {
			if (o instanceof String) {
				String s = (String) o;
				if (s.compareTo("A") == 0)
					base = s;
			}
		}
		return base;
	}

}
