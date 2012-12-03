package org.lttng.studio.model.graph;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

public class TaskExecutionGraph {

	private final DirectedGraph<ExecVertex, ExecEdge> graph;

	public TaskExecutionGraph() {
		graph = new DefaultDirectedGraph<ExecVertex, ExecEdge>(ExecEdge.class);
	}

	public DirectedGraph<ExecVertex, ExecEdge> getGraph() {
		return graph;
	}

}
