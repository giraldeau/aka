package org.lttng.studio.model.graph;

import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.lttng.studio.collect.BinarySearch;
import org.lttng.studio.reader.handler.ALog;

public class DepthFirstCriticalPathAnnotation {

	public DepthFirstCriticalPathAnnotation() {
	}

	public static List<ExecEdge> computeCriticalPath(ExecGraph graph, ExecVertex start) {
		return computeCriticalPath(graph, start, new ALog());
	}

	public static List<ExecEdge> computeCriticalPath(ExecGraph graph, ExecVertex start, ALog log) {
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

			// detect blocking
			if (path.peek().getType() == EdgeType.BLOCKED && splits.isEmpty()) {
				log.debug("blocking ahead, splits empty, search for new head");
				ExecVertex mergeVertex = graph.getGraph().getEdgeTarget(path.peek());
				Set<ExecEdge> incomingEdges = graph.getGraph().incomingEdgesOf(mergeVertex);
				ExecEdge mergeEdge = null;
				for (ExecEdge edge: incomingEdges) {
					ExecVertex source = graph.getGraph().getEdgeSource(edge);
					ExecVertex target = graph.getGraph().getEdgeTarget(edge);
					if (source.getOwner() != target.getOwner()) {
						mergeEdge = edge;
						break;
					}
				}
				if (mergeEdge == null) {
					log.debug("no merge edge found, break");
					break;
				}
				log.debug("clear path");
				path.clear();

				// roll-back
				ExecVertex source = graph.getGraph().getEdgeSource(mergeEdge);
				List<ExecVertex> list = graph.getVertexMap().get(source.getOwner());
				int index = BinarySearch.floor(list, start);
				next = list.get(index);
				log.debug("changing curr to " + curr);
			} else if (path.peek().getType() == EdgeType.BLOCKED || selfEdge == null) {
				if (path.peek().getType() == EdgeType.BLOCKED)
					log.debug("blocking ahead");
				if (selfEdge == null)
					log.debug("dead-end ahead");
				if (splits.isEmpty()) {
					log.debug("splits empty, break");
					break;
				} else {
					ExecVertex top = splits.pop();
					// rewind path to top split vertex
					log.debug("rewind path until " + top);
					while(!path.isEmpty() && graph.getGraph().getEdgeTarget(path.peek()) != top) {
						ExecEdge edge = path.pop();
						log.debug("pop " + edge);
					}
					if (!path.isEmpty())
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
				}
			} else {
				next = graph.getGraph().getEdgeTarget(selfEdge);
				log.debug("self next " + next);
			}
			if (curr.getOwner() != next.getOwner()) {
				log.debug("switch actor from " + curr.getOwner() + " to " + next.getOwner());
			}
			if (curr == next) {
				log.debug("the algorithm do not progress, break");
				break;
			}
			curr = next;
			log.debug("stack splits:");
			for (ExecVertex split: splits) {
				log.debug("  " + split);
			}
			log.debug("stack path:");
			for (ExecEdge edge: path) {
				log.debug("  " + edge);
			}
		}
		propagateParentOwner(graph, path);
		return path;
	}

	private static void propagateParentOwner(ExecGraph graph, List<ExecEdge> path) {
		// TODO
	}

}
