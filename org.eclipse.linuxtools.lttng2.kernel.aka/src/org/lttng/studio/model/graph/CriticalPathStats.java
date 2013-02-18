package org.lttng.studio.model.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.traverse.AbstractGraphIterator;

public class CriticalPathStats {

	public static final double NANO = 1000000000.0;

	public static String formatStats(Collection<Span> spans) {
		StringBuilder str = new StringBuilder();
		for (Span span: spans) {
			str.append(String.format("%s %d\n", span.getOwner(), span.getTotal()));
		}
		return str.toString();
	}

	public static HashMap<Object, Span> compile(ExecGraph graph, ExecVertex start) {
		HashMap<Object, Span> spanMap = new HashMap<Object, Span>();
		List<ExecEdge> path = computeCriticalPath(graph, start);
		for (ExecEdge edge: path) {
			switch (edge.getType()) {
			case DEFAULT:
			case RUNNING:
				ExecVertex source = graph.getGraph().getEdgeSource(edge);
				ExecVertex target = graph.getGraph().getEdgeTarget(edge);
				if (source.getOwner() != target.getOwner())
					throw new RuntimeException("edge " + edge.getType() + " must have same endpoints owner");
				// get or create span
				Span span = spanMap.get(source.getOwner());
				if (span == null) {
					span = new Span(source.getOwner());
					spanMap.put(source.getOwner(), span);
				}
				// update statistics
				long duration = target.getTimestamp() - source.getTimestamp();
				System.out.println(edge + " " + duration + " " + spanMap);
				span.addSelf(duration);
				break;
			case MERGE:
			case SPLIT:
			case INTERRUPTED:
			case MESSAGE:
			case BLOCKED:
			default:
				break;
			}
		}
		return spanMap;
	}

	public static List<ExecEdge> computeCriticalPath(ExecGraph graph, ExecVertex start) {
		ArrayList<ExecEdge> result = new ArrayList<ExecEdge>();
		final ArrayList<ExecEdge> sorted = new ArrayList<ExecEdge>();
		if (!graph.getGraph().vertexSet().contains(start))
			return result;
		ClosestFirstCriticalPathAnnotation traversal = new ClosestFirstCriticalPathAnnotation(graph);
		AbstractGraphIterator<ExecVertex, ExecEdge> iter =
				new ForwardClosestIterator<ExecVertex, ExecEdge>(graph.getGraph(), start);
		iter.addTraversalListener(traversal);
		iter.addTraversalListener(new TraversalListenerAdapter<ExecVertex, ExecEdge>() {
			@Override
			public void edgeTraversed(EdgeTraversalEvent<ExecVertex, ExecEdge> item) {
				sorted.add(item.getEdge());
			}
		});
		while (iter.hasNext() && !traversal.isDone())
			iter.next();
		HashMap<ExecEdge, Integer> map = traversal.getEdgeState();
		for (ExecEdge edge: sorted) {
			if (map.get(edge) == ExecEdge.RED) {
				result.add(edge);
			}
		}
		return result;
	}

}
