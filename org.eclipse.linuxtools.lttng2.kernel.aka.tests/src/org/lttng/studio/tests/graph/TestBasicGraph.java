package org.lttng.studio.tests.graph;

import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.traverse.AbstractGraphIterator;
import org.jgrapht.traverse.ClosestFirstIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.junit.Test;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecVertex;

public class TestBasicGraph {

	@Test
	public void testGraphClosestFirstTraversal() {
		final DirectedWeightedMultigraph<ExecVertex, ExecEdge> graph = BasicGraph.makeLengthUnequal();
		//AbstractGraphIterator<ExecVertex, ExecEdge> iter = new TopologicalOrderIterator<ExecVertex, ExecEdge>(graph);
		AbstractGraphIterator<ExecVertex, ExecEdge> iter = new ClosestFirstIterator<ExecVertex, ExecEdge>(graph);
		iter.addTraversalListener(new TraversalListenerAdapter<ExecVertex, ExecEdge>() {
			@Override
			public void vertexTraversed(VertexTraversalEvent<ExecVertex> item) {
				System.out.println("vertexTraversed " + item.getVertex());
			}
		});
		
		while(iter.hasNext())
			iter.next();
	}
	
}
