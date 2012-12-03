package org.lttng.studio.model.graph;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.lttng.studio.model.kernel.Task;

public class TaskHierarchyGraph {

	private final DirectedGraph<Task, DefaultEdge> graph;

	public TaskHierarchyGraph() {
		graph = new DefaultDirectedGraph<Task, DefaultEdge>(DefaultEdge.class);
	}

	public DirectedGraph<Task, DefaultEdge> getGraph() {
		return graph;
	}

}
