package org.lttng.studio.tests.graph;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
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
		exp.put(BasicGraph.GRAPH_BASIC, 		"A0 A1 B1 B2 A2 A3");
		exp.put(BasicGraph.GRAPH_CONCAT, 		"A0 A1 B1 B2 A2 A3 C3 C4 A4 A5");
		exp.put(BasicGraph.GRAPH_EMBEDED, 		"A0 A1 C1 A2 B2 B3 A3 C4 A4 A5");
		exp.put(BasicGraph.GRAPH_INTERLEAVE, 	"A0 A1 B1 A2 C2 B3 A3 C4 A4 A5");
		exp.put(BasicGraph.GRAPH_NESTED,		"A0 A1 B1 B2 C2 C3 B3 B4 A4 A5");
		exp.put(BasicGraph.GRAPH_OPEN1, 		"A0 A1 B1 A2 B2");
		exp.put(BasicGraph.GRAPH_OPEN2, 		"A0 A1 A2");
		exp.put(BasicGraph.GRAPH_SHELL, 		"A0 A1 B1 A2 B2 C2 A3 C3 B3 D3 A4 D4 " +
												"C4 B4 C5 B5 A5 D5 E5 D6 E6 B6 C6 A6 " + 
												"C7 A7 E7 D7 B7 D8 A8 C8 B8 E8 B9 E9 " + 
												"A9 D9 C9 D10 C10 E10 B10 A10 A11 D11 " + 
												"C11 B11 E11 B12 E12 D12 A12 C12 C13 " + 
												"B13 E13 A13 D13 A14 D14 B14 C14 E14 " +
												"C15 E15 D15 A15 B15 A16 B16 E16 C16 " + 
												"D16 C17 D17 B17 A17 E17 A18 E18 C18 " + 
												"B18 D18");
		// FIXME: avoid non-determinism in test results (because order of
		// same rank vertex is not guarantee)
		Set<String> graphName = BasicGraph.getGraphName();
		for (String name: graphName) {
			ExecGraph graph = BasicGraph.makeGraphByName(name);
			String str = getForwardClosestTraversalString(graph);
			//System.out.println(String.format("%20s %s", name, str));
			assertTrue(str.toString().matches(exp.get(name)));
		}
	}
	
	private String getForwardClosestTraversalString(ExecGraph graph) {
		// retrieve the base object
		Object base = getBaseObject(graph);

		final StringBuilder str = new StringBuilder();
		ExecVertex tail = graph.getStartVertexOf(base);
		AbstractGraphIterator<ExecVertex, ExecEdge> iter = 
				new ForwardClosestIterator<ExecVertex, ExecEdge>(graph.getGraph(), tail);
		iter.addTraversalListener(new TraversalListenerAdapter<ExecVertex, ExecEdge>() {
			@Override
			public void vertexTraversed(VertexTraversalEvent<ExecVertex> item) {
				ExecVertex v = item.getVertex();
				if (str.length() != 0)
					str.append(" ");
				str.append("" + v.getOwner() + v.getTimestamp());
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
