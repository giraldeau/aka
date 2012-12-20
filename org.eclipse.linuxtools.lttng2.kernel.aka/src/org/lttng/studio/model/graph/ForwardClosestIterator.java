package org.lttng.studio.model.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.traverse.CrossComponentIterator;

/*
 * The reverse graph iterator visits vertex from end to start through
 * the lightweight edge.
 */
public class ForwardClosestIterator<V extends Comparable<V>, E> extends CrossComponentIterator<V, E, Object> {

	protected PriorityQueue<V> queue;
	protected DirectedGraph<V, E> graph;
	protected HashSet<E> seenEdge = new HashSet<E>();
	protected HashSet<V> seenVertex = new HashSet<V>();
	
	public ForwardClosestIterator(DirectedGraph<V, E> g, V startVertex) {
		super(g, startVertex);
		queue = new PriorityQueue<V>(8);
		this.graph = g;
		if (!graph.containsVertex(startVertex))
			throw new RuntimeException("start vertex must be part of the graph");
		queue.add(startVertex);
		setCrossComponentTraversal(false);
	}

	@Override
	public V next() {
		if (hasNext()) {
			V nextVertex = provideNextVertex();
			if (nListeners != 0) {
				Set<E> edges = graph.incomingEdgesOf(nextVertex);
				for (E edge: edges) {
					if (seenEdge.contains(edge))
						fireEdgeTraversed(createEdgeTraversalEvent(edge));
				}
				fireVertexTraversed(createVertexTraversalEvent(nextVertex));
			}
			addChildrenOf(nextVertex);
			return nextVertex;
		} else {
			throw new NoSuchElementException();
		}
	}

	private void addChildrenOf(V vertex) {
		Set<E> edges = graph.outgoingEdgesOf(vertex);
		if (edges == null)
			return;
		for (E edge: edges) {
			V next = Graphs.getOppositeVertex(graph, edge, vertex);
			if (seenVertex.contains(next)) {
				encounterVertexAgain(next, edge);
			} else {
				encounterVertex(next, edge);
			}
		}
	}

	@Override
	public boolean hasNext() {
		return !queue.isEmpty();
	}
	
	@Override
	protected void encounterVertex(V vertex, E edge) {
		if (vertex != null) {
			queue.add(vertex);
			seenVertex.add(vertex);
		}
		if (edge != null)
			seenEdge.add(edge);
	}
	
	@Override
	protected void encounterVertexAgain(V vertex, E edge) {
		if (edge != null)
			seenEdge.add(edge);
	}

	@Override
	protected boolean isConnectedComponentExhausted() {
		return queue.isEmpty();
	}

	@Override
	protected V provideNextVertex() {
		List<V> tmp = new ArrayList<V>();
		V curr = null;
		V next = null;
		while (true) {
			curr = queue.poll();
			
			// last node
			if (queue.isEmpty())
				break;
			
			// order nodes based on visited edges only if
			// they have different rank
			next = queue.peek();
			if (next.compareTo(curr) > 0)
				break;

			// check incoming edges
			Set<E> inEdge = graph.incomingEdgesOf(curr);
			if (inEdge.isEmpty() || seenEdge.containsAll(inEdge))
				break;
			tmp.add(curr);
		}
		// re-inject incomplete nodes
		if (!tmp.isEmpty())
			queue.addAll(tmp);
		return curr;
	}
	
	protected EdgeTraversalEvent<V, E> createEdgeTraversalEvent(E edge) {
		return new EdgeTraversalEvent<V, E>(this, edge);
	}
	
	protected VertexTraversalEvent<V> createVertexTraversalEvent(V vertex) {
		return new VertexTraversalEvent<V>(this, vertex);
	}
	
}
