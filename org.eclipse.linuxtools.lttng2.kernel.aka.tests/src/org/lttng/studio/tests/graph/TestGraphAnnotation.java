package org.lttng.studio.tests.graph;

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
		ExecGraph graph = BasicGraph.makeOpened2();
		CriticalPathAnnotation traversal = new CriticalPathAnnotation();
		ExecVertex head = graph.getStartVertexOf(graph);
		AbstractGraphIterator<ExecVertex, ExecEdge> iter = 
				new ForwardClosestIterator<ExecVertex, ExecEdge>(graph.getGraph(), head);
		iter.addTraversalListener(traversal);
		while (iter.hasNext())
			iter.next();
	}
	
}
