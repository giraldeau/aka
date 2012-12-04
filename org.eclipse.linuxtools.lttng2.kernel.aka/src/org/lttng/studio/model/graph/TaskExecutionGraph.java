package org.lttng.studio.model.graph;

import java.util.List;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

import com.google.common.collect.ArrayListMultimap;

public class TaskExecutionGraph {

	private final DirectedGraph<ExecVertex, ExecEdge> graph;
	private final ArrayListMultimap<Object, ExecVertex> vertexMap;

	public TaskExecutionGraph() {
		graph = new DefaultDirectedGraph<ExecVertex, ExecEdge>(ExecEdge.class);
		vertexMap = ArrayListMultimap.create();
	}

	public DirectedGraph<ExecVertex, ExecEdge> getGraph() {
		return graph;
	}

	public ArrayListMultimap<Object, ExecVertex> getVertexMap() {
		return vertexMap;
	}

	public ExecVertex getStartVertexOf(Object owner) {
		ExecVertex start = null;
		List<ExecVertex> list = vertexMap.get(owner);
		if (!list.isEmpty()) {
			start = list.get(0);
		}
		return start;
	}

	public ExecVertex getEndVertexOf(Object owner) {
		ExecVertex end = null;
		List<ExecVertex> list = vertexMap.get(owner);
		if (!list.isEmpty()) {
			end = list.get(list.size() - 1);
		}
		return end;
	}

}
