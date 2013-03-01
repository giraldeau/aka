package org.lttng.studio.model.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.jgrapht.traverse.AbstractGraphIterator;
import org.lttng.studio.reader.handler.ALog;

public class CriticalPathStats {

	public static final double NANO = 1000000000.0;
	public static final double NANOINV = 0.000000001;

	public static String formatStats(Span root) {
		HashMap<Object, Span> index = makeOwnerSpanIndex(root);
		return formatStats(index.values());
	}

	public static double computeSumSecond(Collection<Span> list) {
		long sum = 0;
		for (Span span: list) {
			sum += span.getSelfTime();
		}
		return sum * NANOINV;
	}

	public static void printStatsHeader(StringBuilder str) {
		str.append(String.format("%-30s %11s %11s %8s %8s\n", "Object", "Self time", "Inc. time", "% Self", "% Inc."));
	}

	public static String formatStats(Collection<Span> spans) {
		StringBuilder str = new StringBuilder();
		ArrayList<Span> arrayList = new ArrayList<Span>(spans);
		Collections.sort(arrayList);
		Collections.reverse(arrayList);
		double sum = computeSumSecond(spans);
		double sumInv = 0.0;
		if (sum > 0)
			sumInv = 1 / sum;
		if (arrayList.isEmpty()) {
			str.append("SPAN EMPTY\n");
		} else {
			printStatsHeader(str);
			for (Span span: arrayList) {
				// skip root span
				if (span.getParent() == null)
					continue;
				printSpanOwner(str, span, 0);
				str.append(String.format("%8.9f %8.9f %8.3f %8.3f\n",
						span.getSelfTime() * NANOINV,
						span.getTotalTime() * NANOINV,
						span.getSelfTime() * NANOINV * sumInv * 100.0,
						span.getTotalTime() * NANOINV * sumInv * 100.0));
			}
			str.append(String.format("Total time: %.9f\n\n", sum));
		}
		return str.toString();
	}

	private static void printSpanOwner(StringBuilder str, Span span, int level) {
		int max = 29;
		if (level > max - 10) {
			level = max - 10;
		}
		String s = "";
		for (int i = 0; i < level; i++) s += " ";
		s += span.getOwner().toString();
		if (s.length() > max) {
			s = s.substring(0, max - 3 - level) + "...";
		}
		str.append(String.format("%-30s ", s));
	}

	public static String formatSpanHierarchy(Span root) {
		HashMap<Object, Span> index = makeOwnerSpanIndex(root);
		double sum = computeSumSecond(index.values());
		double sumInv = 0.0;
		if (sum > 0)
			sumInv = 1 / sum;
		StringBuilder str = new StringBuilder();
		str.append("Span hierarchy\n");
		printStatsHeader(str);
		for (Span rootChild: root.getChildren()) {
			formatSpanHierarchyLevel(str, rootChild, sumInv, 0);
		}
		return str.toString();
	}

	public static void formatSpanHierarchyLevel(StringBuilder str, Span span, double sumInv, int level) {
		printSpanOwner(str, span, level);
		str.append(String.format("%8.9f %8.9f %8.3f %8.3f\n",
				span.getSelfTime() * NANOINV,
				span.getTotalTime() * NANOINV,
				span.getSelfTime() * NANOINV * sumInv * 100.0,
				span.getTotalTime() * NANOINV * sumInv * 100.0));
		for (Span child: span.getChildren()) {
			formatSpanHierarchyLevel(str, child, sumInv, level + 1);
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
				current.addSelfTime(duration);
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
		root.computeTotalTime();
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
		return computeCriticalPath(graph, start, null);
	}

	public static List<ExecEdge> computeCriticalPath(ExecGraph graph, ExecVertex start, ALog log) {
		ArrayList<ExecEdge> result = new ArrayList<ExecEdge>();
		if (!graph.getGraph().vertexSet().contains(start))
			return result;
		ClosestFirstCriticalPathAnnotation annotate = new ClosestFirstCriticalPathAnnotation(graph);
		annotate.setLogger(log);
		//PropagateOwnerTraversalListener propagate = new PropagateOwnerTraversalListener(graph);
		AbstractGraphIterator<ExecVertex, ExecEdge> iter =
				new ForwardClosestIterator<ExecVertex, ExecEdge>(graph.getGraph(), start);
		iter.addTraversalListener(annotate);
		//iter.addTraversalListener(propagate);
		while (iter.hasNext() && !annotate.isDone())
			iter.next();
		//HashMap<ExecEdge, Integer> map = annotate.getEdgeState();
		return annotate.getCriticalPath();
	}

}
