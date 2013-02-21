package org.lttng.studio.model.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.traverse.AbstractGraphIterator;

public class CriticalPathStats {

	public static final double NANO = 1000000000.0;

	public static String formatStats(Collection<Span> spans) {
		int max = 29;
		StringBuilder str = new StringBuilder();
		ArrayList<Span> arrayList = new ArrayList<Span>(spans);
		Collections.sort(arrayList);
		Collections.reverse(arrayList);
		long sum = 0;
		for (Span span: arrayList) {
			sum += span.getTotal();
		}
		double sumInv = 1.0 / sum * 100.0;
		if (arrayList.isEmpty()) {
			str.append("SPAN EMPTY\n");
		} else {
			str.append(String.format("%-30s %-11s %8s\n", "Object", "Time (sec)", "% Rel"));
			for (Span span: arrayList) {
				String s = span.getOwner().toString();
				if (s.length() > max) {
					s = s.substring(0, max - 3) + "...";
				}
				str.append(String.format("%-30s %8.9f %8.3f\n", s,
						span.getTotal() * 0.000000001, span.getTotal() * sumInv ));
			}
			str.append(String.format("Total time: %.9f", sum * 0.000000001));
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
				//System.out.println(edge + " " + duration + " " + spanMap);
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
