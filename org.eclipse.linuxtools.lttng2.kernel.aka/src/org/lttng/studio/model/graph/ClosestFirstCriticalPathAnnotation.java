package org.lttng.studio.model.graph;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Set;

import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;

public class ClosestFirstCriticalPathAnnotation extends TraversalListenerAdapter<ExecVertex, ExecEdge> {

	private HashMap<ExecEdge, Integer> edgeState;
	private ExecGraph graph;
	private ExecVertex head; // contains the first encountered verted
	
	public ClosestFirstCriticalPathAnnotation(ExecGraph graph) {
		edgeState = new HashMap<ExecEdge, Integer>();
		this.graph = graph;
	}
	
	@Override
	public void vertexTraversed(VertexTraversalEvent<ExecVertex> item) {
		ExecEdge nextSelf = null;
		/*
		 * 1. set outgoing edges as red
		 *    if at least one incoming edge is red, otherwise set them as blue
		 * 2. get next outgoing edge of the current owner 
		 */
		
		ExecVertex vertex = item.getVertex();
		if (head == null)
			head = vertex;
		int numIn = graph.getGraph().incomingEdgesOf(vertex).size();
		int inRed = countRedEdgeIncoming(vertex);
		int color = (numIn > 0 && inRed == 0) ? ExecEdge.BLUE : ExecEdge.RED;
		//System.out.println("vertex " + vertex + " color " + color);
	
		Set<ExecEdge> out = graph.getGraph().outgoingEdgesOf(vertex);
		for (ExecEdge e: out) {
			edgeState.put(e, color);
			ExecVertex target = graph.getGraph().getEdgeTarget(e);
			if (target.getOwner() == vertex.getOwner()) {
				nextSelf = e;
			}
		}
		
		// dead-end
		if (nextSelf == null) {
			if (vertex.getOwner() != head.getOwner()) {
				annotateBlueBackward(vertex);
			}
			return;
		}
		
		// backtrack if encounter blocking and current color is RED
		if (color == ExecEdge.RED && nextSelf.getType() == EdgeType.BLOCKED) {
			edgeState.put(nextSelf, ExecEdge.BLUE);
			annotateBlueBackward(vertex);
		}
	}

	/* 
	 * Annotate edges as BLUE until a vertex with
	 * 2 red edges is encountered
	 */
	public void annotateBlueBackward(ExecVertex vertex) {
		Deque<ExecVertex> queue = new ArrayDeque<ExecVertex>();
		queue.add(vertex);
		while(true) {
			if (queue.isEmpty())
				break;
			ExecVertex curr = queue.poll();
			int red = countRedEdgeAll(curr);
			if (red >= 2)
				continue;
			Set<ExecEdge> inc = graph.getGraph().incomingEdgesOf(curr);
			for (ExecEdge e: inc) {
				queue.add(graph.getGraph().getEdgeSource(e));
				if (edgeState.containsKey(e)) {
					edgeState.put(e, ExecEdge.BLUE);
				}
			}
		}
	}
	
	public int countRedEdgeAll(ExecVertex vertex) {
		return countRedEdge(graph.getGraph().edgesOf(vertex));
	}
	
	public int countRedEdgeIncoming(ExecVertex vertex) {
		return countRedEdge(graph.getGraph().incomingEdgesOf(vertex));
	}

	public int countRedEdgeOutgoing(ExecVertex vertex) {
		return countRedEdge(graph.getGraph().outgoingEdgesOf(vertex));
	}
	
	public int countRedEdge(Set<ExecEdge> set) {
		int red = 0;
		for (ExecEdge e: set) {
			if (edgeState.get(e) == ExecEdge.RED)
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
