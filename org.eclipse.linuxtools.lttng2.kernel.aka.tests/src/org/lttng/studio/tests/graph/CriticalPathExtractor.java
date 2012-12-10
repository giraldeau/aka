package org.lttng.studio.tests.graph;

import java.util.HashSet;

import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.Subgraph;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecVertex;

public class CriticalPathExtractor {
	public Subgraph<ExecVertex, ExecEdge, DirectedWeightedMultigraph<ExecVertex, ExecEdge>> compute(
			DirectedWeightedMultigraph<ExecVertex, ExecEdge> graph,
			ExecVertex start) {
		HashSet<ExecVertex> v = new HashSet<ExecVertex>();
		HashSet<ExecEdge> e = new HashSet<ExecEdge>();

		
		return new Subgraph<ExecVertex, ExecEdge, DirectedWeightedMultigraph<ExecVertex, ExecEdge>>(
				graph, v, e);
	}
}
