package org.lttng.studio.model.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.traverse.AbstractGraphIterator;
import org.lttng.studio.collect.BinarySearch;
import org.lttng.studio.reader.handler.ALog;

public class ClosestFirstCriticalPathAnnotation extends TraversalListenerAdapter<ExecVertex, ExecEdge> {

	private final HashMap<ExecEdge, Integer> edgeState;
	private final ExecGraph graph;
	private ExecVertex headVertex; // first encountered vertex
	private ExecVertex criticalPathStartVertex; // critical path start
	private final HashMap<Object, ExecVertex> tail;
	private boolean done;
	private ALog log;
	private final HashMap<Object, Object> latestSplitMap;
	private boolean bootstrap;

	public ClosestFirstCriticalPathAnnotation(ExecGraph graph) {
		latestSplitMap = new HashMap<Object, Object>();
		edgeState = new HashMap<ExecEdge, Integer>();
		this.graph = graph;
		tail = new HashMap<Object, ExecVertex>();
		setDone(false);
		bootstrap = true;
	}

	@Override
	public void vertexTraversed(VertexTraversalEvent<ExecVertex> item) {
		ExecEdge nextSelf = null;
		/*
		 * 1. set outgoing edges as red
		 *    if at least one incoming edge is red, otherwise set them as blue
		 * 2. get next outgoing edge of the current owner
		 */

		ExecVertex vertex = item.getVertex();

		// propagate parent owner
		Object parentOwner = latestSplitMap.get(vertex.getOwner());
		vertex.setParentOwner(parentOwner);
		debug("set parent owner of vertex " + vertex + " to " + parentOwner);

		// detect wakeup from unkown object
		annotateReverseUnkownMerge(vertex);

		if (headVertex == null) {
			headVertex = vertex;
			criticalPathStartVertex = vertex;
		}
		int color = ExecEdge.RED;
		if (!bootstrap) {
			int numIn = graph.getGraph().incomingEdgesOf(vertex).size();
			int inRed = countRedEdgeIncoming(vertex);
			color = (numIn > 0 && inRed == 0) ? ExecEdge.BLUE : ExecEdge.RED;
			bootstrap = false;
		}
		debug("vertex " + vertex + " color " + color);

		nextSelf = findSelfOutgoingEdge(graph, vertex);

		// dead-end in main actor, stop here and mark dangling path as blue
		if (nextSelf == null && vertex.getOwner() == headVertex.getOwner()) {
			annotateBlueTail();
			setDone(true);
			debug("edgeState " + edgeState);
			return;
		}

		// annotate outgoing edges with inherited color
		Set<ExecEdge> out = graph.getGraph().outgoingEdgesOf(vertex);
		for (ExecEdge e: out) {
			setEdgeState(e, color);
			ExecVertex target = graph.getGraph().getEdgeTarget(e);
			tail.put(target.getOwner(), target);
		}

		// dead-end in sub-path
		if (nextSelf == null) {
			annotateBlueBackward(vertex);
			return;
		}

		// backtrack if encounter blocking and current color is RED
		if (color == ExecEdge.RED && nextSelf.getType() == EdgeType.BLOCKED) {
			setEdgeState(nextSelf, ExecEdge.BLUE);
			annotateBlueBackward(vertex);
		}
	}

	private void setEdgeState(ExecEdge e, int color) {
		if (edgeState.containsKey(e)) {
			debug("update edge state " + e + " color=" + color);
		} else {
			debug("insert edge state " + e + " color=" + color);
		}
		edgeState.put(e, color);
	}

	private ExecEdge findSelfOutgoingEdge(ExecGraph graph2, ExecVertex vertex) {
		Set<ExecEdge> out = graph.getGraph().outgoingEdgesOf(vertex);
		for (ExecEdge e: out) {
			ExecVertex target = graph.getGraph().getEdgeTarget(e);
			if (target.getOwner() == vertex.getOwner()) {
				return e;
			}
		}
		return null;
	}

	private void annotateReverseUnkownMerge(ExecVertex vertex) {
		Set<ExecEdge> inc = graph.getGraph().incomingEdgesOf(vertex);
		ExecEdge unknownMergeEdge = null;
		// assume there are no spurious merge
		for (ExecEdge edge: inc) {
			if (edge.getType() == EdgeType.MERGE && !edgeState.containsKey(edge)) {
				unknownMergeEdge = edge;
				debug("MERGE from unknown object " + edge);
				break;
			}
		}
		if (unknownMergeEdge == null)
			return;
		// roll-back
		Object owner = graph.getGraph().getEdgeSource(unknownMergeEdge).getOwner();
		List<ExecVertex> list = graph.getVertexMap().get(owner);
		int index = BinarySearch.floor(list, criticalPathStartVertex);
		ExecVertex newStartVertex = list.get(index);
		if (newStartVertex == null) {
			debug("newStartVertex == null");
			return;
		} else {
			debug("criticalPathStartVertex " + criticalPathStartVertex);
			debug("newStartVertex          " + newStartVertex);
		}
		bootstrap = true;
		criticalPathStartVertex = newStartVertex;
		// the merge target owner is the parent owner
		latestSplitMap.put(criticalPathStartVertex.getOwner(), vertex.getOwner());

		ExecVertex stopVertex = graph.getGraph().getEdgeSource(unknownMergeEdge);
		ExecEdge startSelfEdge = findSelfOutgoingEdge(graph, criticalPathStartVertex);

		debug("HEAD FOUND " + startSelfEdge + " at index=" + index);
		debug("ANNOTATE START");
		AbstractGraphIterator<ExecVertex, ExecEdge> iter =
				new ForwardClosestIterator<ExecVertex, ExecEdge>(graph.getGraph(), criticalPathStartVertex);
		iter.addTraversalListener(this);
		while (iter.hasNext()) {
			ExecVertex v = iter.next();
			if (v == stopVertex)
				break;
		}
		debug("ANNOTATE END");
	}

	private void annotateBlueTail() {
		HashSet<Object> ownerSet = new HashSet<Object>();
		ownerSet.addAll(tail.keySet());
		for (Object owner: ownerSet) {
			if (owner == headVertex.getOwner())
				continue;
			ExecVertex vertex = tail.remove(owner);
			debug("annotateBlueTail " + vertex);
			annotateBlueBackward(vertex);
		}
	}

	/*
	 * Annotate edges as BLUE until a vertex with
	 * 2 red edges is encountered
	 */
	public void annotateBlueBackward(ExecVertex vertex) {
		Deque<ExecVertex> queue = new ArrayDeque<ExecVertex>();
		queue.add(vertex);
		while(!queue.isEmpty()) {
			ExecVertex curr = queue.poll();
			int red = countRedEdgeAll(curr);
			if (red != 1)
				continue;
			debug("backtrack " + curr);
			Set<ExecEdge> inc = graph.getGraph().incomingEdgesOf(curr);
			for (ExecEdge e: inc) {
				ExecVertex edgeSource = graph.getGraph().getEdgeSource(e);
				if (!queue.contains(edgeSource))
					queue.add(edgeSource);
				if (edgeState.containsKey(e)) {
					if (edgeState.get(e) == ExecEdge.RED) {
						debug("annotateBlue " + e);
						setEdgeState(e, ExecEdge.BLUE);
					}
				}
			}
		}
	}

	public int countRedEdgeAll(ExecVertex vertex) {
		return countRedEdge(graph.getGraph().edgesOf(vertex));
	}

	public int countRedEdgeIncoming(ExecVertex vertex) {
		return countRedEdge(graph.getGraph().incomingEdgesOf(vertex));
	}

	public int countRedEdgeOutgoing(ExecVertex vertex) {
		return countRedEdge(graph.getGraph().outgoingEdgesOf(vertex));
	}

	public int countRedEdge(Set<ExecEdge> set) {
		int red = 0;
		for (ExecEdge e: set) {
			if (edgeState.get(e) == ExecEdge.RED)
				red++;
		}
		return red;
	}

	@Override
	public void edgeTraversed(EdgeTraversalEvent<ExecVertex, ExecEdge> item) {
		ExecEdge edge = item.getEdge();
		if (edge.getType() == EdgeType.SPLIT) {
			ExecVertex source = graph.getGraph().getEdgeSource(edge);
			ExecVertex target = graph.getGraph().getEdgeTarget(edge);
			Object parentOwner = source.getOwner();
			Object owner = target.getOwner();
			latestSplitMap.put(owner, parentOwner);
		}
	}

	public HashMap<ExecEdge, Integer> getEdgeState() {
		return edgeState;
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	private void debug(String string) {
		if (log != null)
			log.debug(string);
	}

	public void setLogger(ALog log) {
		this.log = log;
	}

	public ArrayList<ExecEdge> getCriticalPath() {
		ArrayList<ExecEdge> path = new ArrayList<ExecEdge>();
		for (ExecEdge edge: edgeState.keySet()) {
			if (edgeState.get(edge) == ExecEdge.RED) {
				path.add(edge);
			}
		}
		Collections.sort(path, new ExecEdgeComparator(graph));
		return path;
	}

}