package org.lttng.studio.model.graph;

import java.util.HashMap;
import java.util.Set;
import java.util.Stack;

import org.lttng.studio.reader.handler.ALog;

public class DepthFirstCriticalPathAnnotation {

	public DepthFirstCriticalPathAnnotation() {
	}

	public static HashMap<ExecEdge, Integer> computeCriticalPath(ExecGraph graph, ExecVertex start) {
		return computeCriticalPath(graph, start, new ALog());
	}

	public static HashMap<ExecEdge, Integer> computeCriticalPath(ExecGraph graph, ExecVertex start, ALog log) {
		HashMap<ExecEdge, Integer> edgeState = new HashMap<ExecEdge, Integer>();
		Stack<ExecVertex> splits = new Stack<ExecVertex>();
		Stack<ExecEdge> path = new Stack<ExecEdge>();
		ExecVertex curr = start;
		while(curr != null) {
			log.debug("processing " + curr);
			// identify split and self vertex
			ExecEdge selfEdge = null;
			ExecVertex next = null;
			Set<ExecEdge> out = graph.getGraph().outgoingEdgesOf(curr);

			for (ExecEdge e: out) {
				ExecVertex target = graph.getGraph().getEdgeTarget(e);
				if (target.getOwner() != curr.getOwner()) {
					log.debug("push split vertex " + curr);
					splits.push(curr);
				} else {
					log.debug("push self edge " + e);
					selfEdge = e;
				}
			}

			// stop condition
			if (curr.getOwner() == start.getOwner() && selfEdge == null) {
				log.debug("stop condition reached");
				break;
			}

			if (selfEdge != null)
				path.push(selfEdge);

			// detect blocking or dead-end
			if (path.peek().getType() == EdgeType.BLOCKED || selfEdge == null) {
				log.debug("blocking or dead-end ahead");
				if (splits.isEmpty()) {
					log.debug("splits empty, break, nowhere to go");
					break;
				}
				ExecVertex top = splits.pop();
				// rewind path to top split vertex
				log.debug("rewind path until " + top);
				while(!path.isEmpty() && graph.getGraph().getEdgeTarget(path.peek()) != top) {
					ExecEdge edge = path.pop();
					log.debug("pop " + edge);
				}
				log.debug("path.peek() " + path.peek());
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
				log.debug("switch actor from " + curr.getOwner() + " to " + next.getOwner());
			} else {
				next = graph.getGraph().getEdgeTarget(selfEdge);
				log.debug("self next " + next);
			}
			curr = next;
			log.debug("stack splits " + splits);
			log.debug("stack path   " + path + "\n");
		}
		for (ExecEdge edge: path) {
			edgeState.put(edge, ExecEdge.RED);
		}
		return edgeState;
	}

}
