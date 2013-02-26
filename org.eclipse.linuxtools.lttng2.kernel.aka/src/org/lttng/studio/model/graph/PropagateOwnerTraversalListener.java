package org.lttng.studio.model.graph;

import java.util.HashMap;

import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;

public class PropagateOwnerTraversalListener extends TraversalListenerAdapter<ExecVertex, ExecEdge> {

	final HashMap<Object, Object> latestSplitMap; // (owner, parentOwner)
	private final ExecGraph graph;

	public PropagateOwnerTraversalListener(ExecGraph graph) {
		this.graph = graph;
		latestSplitMap = new HashMap<Object, Object>();
	}

	@Override
	public void vertexTraversed(VertexTraversalEvent<ExecVertex> item) {
		ExecVertex vertex = item.getVertex();
		Object parentOwner = latestSplitMap.get(vertex.getOwner());
		vertex.setParentOwner(parentOwner);
	}

	@Override
	public void edgeTraversed(EdgeTraversalEvent<ExecVertex, ExecEdge> item) {
		ExecEdge edge = item.getEdge();
		if (edge.getType() == EdgeType.SPLIT) {
			ExecVertex source = graph.getGraph().getEdgeSource(edge);
			ExecVertex target = graph.getGraph().getEdgeTarget(edge);
			Object parentOwner = source.getOwner();
			Object owner = target.getOwner();
			latestSplitMap.put(owner, parentOwner);
		}
	}

}
