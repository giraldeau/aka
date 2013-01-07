package org.lttng.studio.model.graph;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Set;

import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;

public class CriticalPathAnnotation extends TraversalListenerAdapter<ExecVertex, ExecEdge> {

	final public static Integer RED = 1;
	final public static Integer BLUE = 2;
	private HashMap<ExecEdge, Integer> edgeState;
	private ExecGraph graph;
	
	public CriticalPathAnnotation(ExecGraph graph) {
		edgeState = new HashMap<ExecEdge, Integer>();
		this.graph = graph;
	}
	
	@Override
	public void vertexTraversed(VertexTraversalEvent<ExecVertex> item) {
		// 1. add all outgoing edge as candidate for critical path
		// 2. get next outgoing edge of the current owner
		ExecEdge next = null;
		ExecVertex vertex = item.getVertex();
		Set<ExecEdge> out = graph.getGraph().outgoingEdgesOf(vertex);
		for (ExecEdge e: out) {
			edgeState.put(e, RED);
			ExecVertex target = graph.getGraph().getEdgeTarget(e);
			if (target.getOwner() == vertex.getOwner()) {
				next = e;
			}
		}
		
		// no more edge to explore
		if (next == null) {
			return;
		}
		
		// backtrack if encounter blocking
		// annotate edges as blue until a vertex with
		// 2 red edges is encountered
		if (next.getType() == EdgeType.BLOCKED) {
			edgeState.put(next, BLUE);
			Deque<ExecVertex> queue = new ArrayDeque<ExecVertex>();
			queue.add(vertex);
			while(true) {
				if (queue.isEmpty())
					break;
				ExecVertex curr = queue.poll();
				int red = countRedEdge(curr);
				if (red >= 2)
					continue;
				Set<ExecEdge> inc = graph.getGraph().incomingEdgesOf(curr);
				for (ExecEdge e: inc) {
					queue.add(graph.getGraph().getEdgeSource(e));
					if (edgeState.containsKey(e)) {
						edgeState.put(e, BLUE);
					}
				}
			}
		}
	}
	
	public int countRedEdge(ExecVertex vertex) {
		Set<ExecEdge> all = graph.getGraph().edgesOf(vertex);
		int red = 0;
		for (ExecEdge e: all) {
			if (edgeState.get(e) == RED)
				red++;
		}
		return red;
	}
	
	@Override
	public void edgeTraversed(EdgeTraversalEvent<ExecVertex, ExecEdge> item) {
	}

	public HashMap<ExecEdge, Integer> getEdgeState() {
		return edgeState;
	}
	
}
