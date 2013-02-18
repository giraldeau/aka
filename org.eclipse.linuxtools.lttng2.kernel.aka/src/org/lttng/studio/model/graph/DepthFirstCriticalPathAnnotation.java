package org.lttng.studio.model.graph;

import java.util.HashMap;
import java.util.Set;
import java.util.Stack;

public class DepthFirstCriticalPathAnnotation {

	public DepthFirstCriticalPathAnnotation() {
	}

	public static HashMap<ExecEdge, Integer> computeCriticalPath(ExecGraph graph, ExecVertex start) {
		HashMap<ExecEdge, Integer> edgeState = new HashMap<ExecEdge, Integer>();
		Stack<ExecVertex> splits = new Stack<ExecVertex>();
		Stack<ExecEdge> path = new Stack<ExecEdge>();
		ExecVertex curr = start;
		while(curr != null) {
			//System.out.println("processing " + curr);
			// identify split and self vertex
			ExecEdge selfEdge = null;
			ExecVertex next = null;
			Set<ExecEdge> out = graph.getGraph().outgoingEdgesOf(curr);

			for (ExecEdge e: out) {
				ExecVertex target = graph.getGraph().getEdgeTarget(e);
				if (target.getOwner() != curr.getOwner()) {
					//System.out.println("push split vertex " + curr);
					splits.push(curr);
				} else {
					//System.out.println("push self edge " + e);
					selfEdge = e;
				}
			}

			// stop condition
			if (curr.getOwner() == start.getOwner() && selfEdge == null) {
				//System.out.println("stop condition reached");
				break;
			}

			if (selfEdge != null)
				path.push(selfEdge);

			// detect blocking or dead-end
			if (path.peek().getType() == EdgeType.BLOCKED || selfEdge == null) {
				//System.out.println("blocking or dead-end ahead");
				if (splits.isEmpty()) {
					//System.out.println("splits empty, break, nowhere to go");
					break;
				}
				ExecVertex top = splits.pop();
				// rewind path to top split vertex
				//System.out.println("rewind path until " + top);
				while(!path.isEmpty() && graph.getGraph().getEdgeTarget(path.peek()) != top) {
					ExecEdge edge = path.pop();
					//System.out.println("pop " + edge);
				}
				//System.out.println("path.peek() " + path.peek());
				// switch actor
				out = graph.getGraph().outgoingEdgesOf(top);
				for (ExecEdge e: out) {
					ExecVertex target = graph.getGraph().getEdgeTarget(e);
					if (target.getOwner() != top.getOwner()) {
						path.push(e);
						next = target;
						break;
					}
				}
				//System.out.println("switch actor from " + curr.getOwner() + " to " + next.getOwner());
			} else {
				next = graph.getGraph().getEdgeTarget(selfEdge);
				//System.out.println("self next " + next);
			}
			curr = next;
			//System.out.println("stack splits " + splits);
			//System.out.println("stack path   " + path);
			//System.out.println("");
		}
		for (ExecEdge edge: path) {
			edgeState.put(edge, ExecEdge.RED);
		}
		return edgeState;
	}

}
