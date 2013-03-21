package org.lttng.studio.model.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.lttng.studio.reader.handler.ALog;

public class DepthFirstCriticalPathBackward {

	ExecGraph graph;
	private final ALog log;
	private HashSet<ExecEdge> visitedEdges;
	private Stack<ExecEdge> path;
	private ExecVertex start;
	private ExecVertex stop;

	public DepthFirstCriticalPathBackward(ExecGraph graph) {
		this(graph, new ALog());
	}

	public DepthFirstCriticalPathBackward(ExecGraph graph, ALog log) {
		this.log = log;
		this.graph = graph;
	}

	public List<ExecEdge> criticalPath(ExecVertex start, ExecVertex stop) {
		this.start = start;
		this.stop = stop;
		path = new Stack<ExecEdge>();
		visitedEdges = new HashSet<ExecEdge>();
		ExecVertex curr = start;
		while(curr != null && curr.getTimestamp() < stop.getTimestamp()) {
			ExecVertex next = null;
			log.debug("processing vertex " + curr);
			if (curr == stop) {
				log.debug("reached stop vertex, break " + curr);
				break;
			}
			ExecEdge self = findEdge(curr, true, true);

			if (self == null) {
				log.debug("self null, break");
				break;
			}

			visitEdge(self);

			log.debug("next edge " + self);
			next = graph.getGraph().getEdgeTarget(self);
			curr = next;
		}
		log.debug("done. critical path found:");
		for (ExecEdge edge: path) {
			log.debug("\t" + edge);
		}
		return path;
	}

	private void dumpList(String header, Collection<? extends Object> list) {
		log.debug("dumpList " + header);
		for (Object e: list) {
			log.debug("\t" + e);
		}
	}

	private void visitEdge(ExecEdge edge) {
		if (visitedEdges.contains(edge)) {
			log.debug("edge already visited " + edge);
			return;
		}
		visitedEdges.add(edge);
		/*
		 * Check for blocking. Follow unblocking origin.
		 */
		if (edge.getType() == EdgeType.BLOCKED) {
			ExecVertex mergeVertex = graph.getGraph().getEdgeTarget(edge);
			ExecEdge mergeEdge = findEdge(mergeVertex, false, false);
			if (mergeEdge == null) {
				log.warning("blocking edge is not followed by merge " + edge);
				appendToPath(path, edge);
				return;
			}
			log.debug("found mergeEdge " + mergeEdge);
			// rewind path until the last split
			/*
			ExecVertex top = origin;
			if (!splits.isEmpty()) {
				top = splits.pop();
			}
			while(!path.isEmpty() && graph.getGraph().getEdgeTarget(path.peek()) != top) {
				ExecEdge topEdge = path.pop();
				log.debug("pop " + topEdge);
			}
			*/
			Stack<ExecEdge> subPath = backward(mergeEdge, 0);
			dumpList("path", path);
			dumpList("subPath", subPath);
			while(!subPath.isEmpty()) {
				appendToPath(path, subPath.remove(0));
			}
		} else {
			appendToPath(path, edge);
		}

	}

	private void appendToPath(Stack<ExecEdge> path, ExecEdge edge) {
		if (!path.isEmpty()) {
			ExecEdge last = path.peek();
			ExecVertex v0 = graph.getGraph().getEdgeTarget(last);
			ExecVertex v1 = graph.getGraph().getEdgeSource(edge);
			if (v0 != v1) {
				log.debug("first=" + last + " v0=" + v0 + " v1=" + v1);
				throw new RuntimeException("append non contiguous segment to critical path");
			}
		}
		path.add(edge);
	}

	private void prependToPath(Stack<ExecEdge> path, ExecEdge edge) {
		if (!path.isEmpty()) {
			ExecEdge first = path.firstElement();
			ExecVertex v0 = graph.getGraph().getEdgeSource(first);
			ExecVertex v1 = graph.getGraph().getEdgeTarget(edge);
			if (v0 != v1) {
				log.debug("edge=" + edge + " first=" + first + " v0=" + v0 + " v1=" + v1);
				throw new RuntimeException("prepend non contiguous segment to critical path");
			}
		}
		path.insertElementAt(edge, 0);
	}

	private interface Conditional<V> {
		public boolean apply(V obj1, V obj2);
	}

	private static Conditional<ExecVertex> equalityCondition = new Conditional<ExecVertex>() {
		@Override
		public boolean apply(ExecVertex obj1, ExecVertex obj2) {
			return obj1 == obj2;
		}
	};

	private static Conditional<ExecVertex> lowerTimeBoundCondition = new Conditional<ExecVertex>() {
		@Override
		public boolean apply(ExecVertex obj1, ExecVertex obj2) {
			return obj1.getTimestamp() <= obj2.getTimestamp();
		}
	};

	private boolean isConnectedTo(Stack<ExecEdge> path, ExecVertex vertex) {
		// FIXME: replace linear search by binary search,
		// since vertex are ordered according to time
		if (!path.isEmpty()) {
			ExecEdge edge = path.firstElement();
			ExecVertex v = graph.getGraph().getEdgeSource(edge);
			if (v == vertex)
				return true;
			for (ExecEdge e: path) {
				v = graph.getGraph().getEdgeTarget(e);
				if (v == vertex)
					return true;
			}
		}
		return false;
	}

	private Stack<ExecEdge> backward(ExecEdge edge, int level) {
		Stack<ExecEdge> subPath = new Stack<ExecEdge>();
		log.debug("BEGIN backward level=" + level + " from edge " + edge);
		ExecVertex prev = null;
		ExecEdge currEdge = edge;
		while(true) {
			if (currEdge == null) {
				log.debug("currEdge null, break");
				break;
			}
			log.debug("processing edge " + currEdge);
			visitedEdges.add(currEdge);
			if (currEdge.getType() == EdgeType.BLOCKED) {
				ExecVertex mergeVertex = graph.getGraph().getEdgeTarget(edge);
				ExecEdge mergeEdge = findEdge(mergeVertex, false, false);
				if (mergeEdge == null) {
					log.warning("blocking edge is not followed by merge " + edge);
					prependToPath(subPath, edge);
				} else {
					log.debug("found mergeEdge " + mergeEdge);
					Stack<ExecEdge> recSubPath = backward(mergeEdge, level + 1);
					while(!recSubPath.isEmpty()) {
						prependToPath(subPath, recSubPath.pop());
					}
				}
			} else {
				prependToPath(subPath, currEdge);
			}
			prev = graph.getGraph().getEdgeSource(currEdge);
			if (prev == null) {
				log.debug("prev vertex null, break");
				break;
			}

			// stop if subPath connects to the main critical path OR
			// do not continue traversing beyond start
			if (isConnectedTo(path, prev)) {
				log.debug("subPath connected to main path");
				// remove path segments until prev is reached
				// parent is responsible to add segments to parent path
				ExecEdge last = path.lastElement();
				ExecVertex v = graph.getGraph().getEdgeTarget(last);
				while(!path.isEmpty() && v != prev) {
					last = path.pop();
					v = graph.getGraph().getEdgeSource(last);
				}
				break;
			}
			if (lowerTimeBoundCondition.apply(prev, start)) {
				log.debug("time bound reached");
				path.clear();
				break;
			}
			// give priority to link edge
			ExecEdge prevEdge = findEdge(prev, false, false);
			if (prevEdge == null) {
				prevEdge = findEdge(prev, true, false);
			}
			if (prevEdge == currEdge) {
				log.debug("algorithm stalls, break");
				break;
			}
			currEdge = prevEdge;
		}
		log.debug("END backward level=" + level);
		return subPath;
	}

	private ExecEdge findEdge(ExecVertex vertex, boolean sameOwner, boolean outgoing) {
		Set<ExecEdge> edges;
		if (outgoing) {
			edges = graph.getGraph().outgoingEdgesOf(vertex);
		} else {
			edges = graph.getGraph().incomingEdgesOf(vertex);
		}
		for (ExecEdge e: edges) {
			ExecVertex endpoint;
			if (outgoing) {
				endpoint = graph.getGraph().getEdgeTarget(e);
			} else {
				endpoint = graph.getGraph().getEdgeSource(e);
			}
			if (sameOwner && endpoint.getOwner() == vertex.getOwner()) {
				return e;
			} else if (!sameOwner && endpoint.getOwner() != vertex.getOwner()) {
				return e;
			}
		}
		return null;
	}

}
