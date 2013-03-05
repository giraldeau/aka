package org.lttng.studio.model.graph;

import java.util.Comparator;

public class ExecEdgeComparator implements Comparator<ExecEdge> {

	private final ExecGraph graph;

	public ExecEdgeComparator(ExecGraph graph) {
		this.graph = graph;
	}

	@Override
	public int compare(ExecEdge e1, ExecEdge e2) {
		ExecVertex v1 = graph.getGraph().getEdgeSource(e1);
		ExecVertex v2 = graph.getGraph().getEdgeSource(e2);
		int inCmp = v1.compareTo(v2);
		if (inCmp == 0) {
			v1 = graph.getGraph().getEdgeTarget(e1);
			v2 = graph.getGraph().getEdgeTarget(e2);
			return v1.compareTo(v2);
		}
		return inCmp;
	}
}