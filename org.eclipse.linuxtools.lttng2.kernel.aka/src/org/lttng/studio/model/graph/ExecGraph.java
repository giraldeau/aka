package org.lttng.studio.model.graph;

import java.util.List;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.DefaultDirectedGraph;

import com.google.common.collect.ArrayListMultimap;

public class ExecGraph {

	private final DirectedGraph<ExecVertex, ExecEdge> graph;
	private final ArrayListMultimap<Object, ExecVertex> vertexMap;

	public ExecGraph() {
		graph = new DefaultDirectedGraph<ExecVertex, ExecEdge>(new EdgeFactory<ExecVertex, ExecEdge>() {
				@Override
				public ExecEdge createEdge(ExecVertex a, ExecVertex b) {
					if (a.getTimestamp() > b.getTimestamp())
						throw new RuntimeException("Error: timstamps A is greater than timestamps b, time must always increase");
					return new ExecEdge(b.getTimestamp() - a.getTimestamp());
				}
		});
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
	
	public void appendVertexByOwner(ExecVertex vertex) {
		graph.addVertex(vertex);
		ExecVertex tail = getEndVertexOf(vertex.getOwner());
		if (tail != null) {
			graph.addEdge(tail, vertex);
		}
		vertexMap.put(vertex.getOwner(), vertex);
	}

	public void addVerticalEdge(ExecVertex src, ExecVertex dst) {
		if (src.getTimestamp() != dst.getTimestamp())
			throw new RuntimeException("Vertical edge source and testination vertex must have equal timestamps");
		graph.addEdge(src, dst);
	}
	
}
