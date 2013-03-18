package org.lttng.studio.model.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lttng.studio.reader.handler.ALog;

public class DepthFirstCriticalPathBackward {

	ExecGraph graph;
	private final ALog log;
	private HashSet<ExecEdge> visitedEdges;
	private ArrayList<ExecEdge> path;

	public DepthFirstCriticalPathBackward(ExecGraph graph) {
		this(graph, new ALog());
	}

	public DepthFirstCriticalPathBackward(ExecGraph graph, ALog log) {
		this.log = log;
		this.graph = graph;
	}

	public List<ExecEdge> criticalPath(ExecVertex start, ExecVertex stop) {
		path = new ArrayList<ExecEdge>();
		visitedEdges = new HashSet<ExecEdge>();
		ExecVertex curr = start;
		while(curr != null && curr.getTimestamp() < stop.getTimestamp()) {
			ExecVertex next = null;
			log.debug("processing vertex " + curr);
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
		log.debug("" + path);
		return path;
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

			}
			log.debug("merge_edge " + mergeEdge);
		} else {
			path.add(edge);
		}

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
