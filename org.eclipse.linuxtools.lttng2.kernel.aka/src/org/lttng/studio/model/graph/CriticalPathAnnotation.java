package org.lttng.studio.model.graph;

import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;

public class CriticalPathAnnotation extends TraversalListenerAdapter<ExecVertex, ExecEdge> {

	@Override
	public void vertexTraversed(VertexTraversalEvent<ExecVertex> item) {
		System.out.println(item.getVertex());
	}
	
	@Override
	public void edgeTraversed(EdgeTraversalEvent<ExecVertex, ExecEdge> item) {
		System.out.println(item.getEdge());
	}
	
}
