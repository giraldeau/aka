package org.lttng.studio.tests.graph;

import java.util.HashMap;

import org.jgrapht.traverse.AbstractGraphIterator;
import org.junit.Test;
import org.lttng.studio.model.graph.CriticalPathAnnotation;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecGraph;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.graph.ForwardClosestIterator;

public class TestGraphAnnotation {

	@Test
	public void testGraphAnnotateBasic() {
		ExecGraph graph = BasicGraph.makeEmbeded();
		ExecVertex base = BasicGraph.getVertexByName(graph, "A0");
		CriticalPathAnnotation traversal = new CriticalPathAnnotation(graph);
		ExecVertex head = graph.getStartVertexOf(base.getOwner());
		AbstractGraphIterator<ExecVertex, ExecEdge> iter = 
				new ForwardClosestIterator<ExecVertex, ExecEdge>(graph.getGraph(), head);
		iter.addTraversalListener(traversal);
		while (iter.hasNext())
			iter.next();
		HashMap<ExecEdge, Integer> map = traversal.getEdgeState();
		for (ExecEdge edge: map.keySet()) {
			System.out.println(edge + " " + map.get(edge));
		}
	}
	
}
