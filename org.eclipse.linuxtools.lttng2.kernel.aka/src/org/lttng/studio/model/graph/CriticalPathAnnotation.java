package org.lttng.studio.model.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
		System.out.println("--------------START " + item.getVertex());
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
		
		if (next == null) {
			System.out.println("next null for " + vertex);
			return;
		}
		
		// backtrack if encounter blocking
		if (next.getType() == EdgeType.BLOCKED) {
			edgeState.put(next, BLUE);
			System.out.println("blocking edge ahead " + vertex + " " + next);
			// union of all edges of the current node
			Deque<ExecVertex> queue = new ArrayDeque<ExecVertex>();
			queue.add(vertex);
			while(true) {
				if (queue.isEmpty())
					break;
				ExecVertex curr = queue.poll();
				System.out.println("process " + curr);
				int red = countRedEdge(curr);
				if (red >= 2)
					continue;
				System.out.println("backtracking!");
				Set<ExecEdge> inc = graph.getGraph().incomingEdgesOf(curr);
				for (ExecEdge e: inc) {
					queue.add(graph.getGraph().getEdgeSource(e));
					if (edgeState.containsKey(e)) {
						System.out.println("set blue edge " + e);
						edgeState.put(e, BLUE);
					}
				}
			}
		}
		System.out.println("--------------END " + vertex);
	}
	
	public int countRedEdge(ExecVertex vertex) {
		Set<ExecEdge> all = graph.getGraph().edgesOf(vertex);
		int red = 0;
		int blue = 0;
		for (ExecEdge e: all) {
			System.out.println("edge " + e + " " + edgeState.get(e));
			if (edgeState.get(e) == RED)
				red++;
			if (edgeState.get(e) == BLUE)
				blue++;
		}
		System.out.println("RED = " + red + " BLUE = " + blue);
		return red;
	}
	
	@Override
	public void edgeTraversed(EdgeTraversalEvent<ExecVertex, ExecEdge> item) {
	}

	public HashMap<ExecEdge, Integer> getEdgeState() {
		return edgeState;
	}
	
}
