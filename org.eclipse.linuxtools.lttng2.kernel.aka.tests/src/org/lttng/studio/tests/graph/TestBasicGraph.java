package org.lttng.studio.tests.graph;

import static org.junit.Assert.assertTrue;

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

	//@Test
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
		Set<String> graphName = BasicGraph.getGraphName();
		for (String name: graphName) {
			ExecGraph graph = BasicGraph.makeGraphByName(name);
			String str = getForwardClosestTraversalString(graph);
			System.out.println(String.format("%20s %s", name, str));			
		}
		// B3 should be visited before A3
		//assertTrue(str.toString().matches("A1 B1 B2 B3 A3 B4"));
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
