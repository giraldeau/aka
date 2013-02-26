package org.lttng.studio.model.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.traverse.AbstractGraphIterator;

public class CriticalPathStats {

	public static final double NANO = 1000000000.0;

	public static String formatStats(Span root) {
		HashMap<Object, Span> index = makeOwnerSpanIndex(root);
		return formatStats(index.values());
	}

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
				// skip root span
				if (span.getParent() == null)
					continue;
				String s = span.getOwner().toString();
				if (s.length() > max) {
					s = s.substring(0, max - 3) + "...";
				}
				str.append(String.format("%-30s %8.9f %8.3f\n", s,
						span.getTotal() * 0.000000001, span.getTotal() * sumInv ));
			}
			str.append(String.format("Total time: %.9f\n", sum * 0.000000001));
		}
		return str.toString();
	}

	public static String formatSpanHierarchy(Span root) {
		StringBuilder str = new StringBuilder();
		str.append("Span hierarchy\n");
		formatSpanHierarchyLevel(str, root, 0);
		return str.toString();
	}

	public static void formatSpanHierarchyLevel(StringBuilder str, Span span, int level) {
		for (int i = 0; i < level; i++) str.append("    ");
		str.append(span.getParent() == null ? "root" : span.getOwner().toString());
		str.append("\n");
		for (Span child: span.getChildren()) {
			formatSpanHierarchyLevel(str, child, level + 1);
		}
	}

	public static Span compile(ExecGraph graph, ExecVertex start) {
		Span root = new Span(new Object());
		// FIXME: Multimap to hold more than one Span per owner in
		// the case there would be different parent owner
		HashMap<Object, Span> spanMap = new HashMap<Object, Span>();
		List<ExecEdge> path = computeCriticalPath(graph, start);
		for (ExecEdge edge: path) {
			ExecVertex source = graph.getGraph().getEdgeSource(edge);
			ExecVertex target = graph.getGraph().getEdgeTarget(edge);
			switch (edge.getType()) {
			case DEFAULT:
			case RUNNING:
				if (source.getOwner() != target.getOwner())
					throw new RuntimeException("edge " + edge.getType() + " must have same endpoints owner");
				// get or create span
				Span current = spanMap.get(source.getOwner());
				if (current == null) {
					current = new Span(source.getOwner());
					Span parentSpan = root;
					if (spanMap.containsKey(source.getParentOwner())) {
						 parentSpan = spanMap.get(source.getParentOwner());
					}
					current.setParentAndChild(parentSpan);
					spanMap.put(source.getOwner(), current);
				}
				// update statistics
				long duration = target.getTimestamp() - source.getTimestamp();
				current.addSelf(duration);
				break;
			case SPLIT:
			case MERGE:
				// change current span to target owner
				Object owner = target.getOwner();
				Object parent = target.getParentOwner();
				Span parentSpan = root;
				if (spanMap.containsKey(parent)) {
					parentSpan = spanMap.get(parent);
				}
				Span span = spanMap.get(owner);
				if (span == null) {
					span = new Span(owner);
					span.setParentAndChild(parentSpan);
					spanMap.put(owner, span);
				}
				break;
			case INTERRUPTED:
			case MESSAGE:
			case BLOCKED:
			default:
				break;
			}
		}
		return root;
	}

	public static HashMap<Object, Span> makeOwnerSpanIndex(Span root) {
		HashMap<Object, Span> spanIndex = new HashMap<Object, Span>();
		spanIndex.put(root.getOwner(), root);
		Stack<Span> stack = new Stack<Span>();
		stack.push(root);
		while (!stack.isEmpty()) {
			Span span = stack.pop();
			spanIndex.put(span.getOwner(), span);
			stack.addAll(span.getChildren());
		}
		return spanIndex;
	}

	public static List<ExecEdge> computeCriticalPath(ExecGraph graph, ExecVertex start) {
		ArrayList<ExecEdge> result = new ArrayList<ExecEdge>();
		final ArrayList<ExecEdge> sorted = new ArrayList<ExecEdge>();
		if (!graph.getGraph().vertexSet().contains(start))
			return result;
		ClosestFirstCriticalPathAnnotation annotate = new ClosestFirstCriticalPathAnnotation(graph);
		PropagateOwnerTraversalListener propagate = new PropagateOwnerTraversalListener(graph);
		AbstractGraphIterator<ExecVertex, ExecEdge> iter =
				new ForwardClosestIterator<ExecVertex, ExecEdge>(graph.getGraph(), start);
		iter.addTraversalListener(annotate);
		iter.addTraversalListener(propagate);
		iter.addTraversalListener(new TraversalListenerAdapter<ExecVertex, ExecEdge>() {
			@Override
			public void edgeTraversed(EdgeTraversalEvent<ExecVertex, ExecEdge> item) {
				sorted.add(item.getEdge());
			}
		});
		while (iter.hasNext() && !annotate.isDone())
			iter.next();
		HashMap<ExecEdge, Integer> map = annotate.getEdgeState();
		for (ExecEdge edge: sorted) {
			if (map.get(edge) == ExecEdge.RED) {
				result.add(edge);
			}
		}
		return result;
	}

}
