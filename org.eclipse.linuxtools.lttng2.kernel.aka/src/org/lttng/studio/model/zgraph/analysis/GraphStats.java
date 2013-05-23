package org.lttng.studio.model.zgraph.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math.stat.descriptive.StatisticalSummary;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.zgraph.Graph;
import org.lttng.studio.model.zgraph.Node;

import com.google.common.collect.HashBiMap;

public class GraphStats {

	private final Graph graph;

	private final HashBiMap<Object, SummaryStatisticsW> statsTotal;

	private static class SummaryStatisticsW extends SummaryStatistics {
		private static final long serialVersionUID = -7946520096569292423L;
		public Object ref = null;
	}

	public GraphStats(Graph g) {
		this.graph = g;
		statsTotal = HashBiMap.create();
	}

	public StatisticalSummary getStat(Object obj) {
		if (obj == null)
			return null;
		compute(obj);
		return statsTotal.get(obj);
	}

	// FIXME: handle vertical link with non-zero duration
	// One solution could be to create one global entry
	private void compute(Object obj) {
		if (statsTotal.containsKey(obj))
			return;
		SummaryStatisticsW stats = new SummaryStatisticsW();
		stats.ref = obj;
		statsTotal.put(obj, stats);
		List<Node> nodes = graph.getNodesOf(obj);
		for (Node node: nodes) {
			if (node.hasNeighbor(Node.RIGHT)) {
				stats.addValue(node.links[Node.RIGHT].duration());
			}
		}
	}

	public String dump() {
		StringBuilder str = new StringBuilder();
		// make sure all stats are computed
		for (Object key: graph.getNodesMap().keySet()) {
			compute(key);
		}

		// sort statistics according to sum
		List<SummaryStatisticsW> sorted = new ArrayList<SummaryStatisticsW>(statsTotal.values());
		Collections.sort(sorted, new Comparator<SummaryStatisticsW>() {
			@Override
			public int compare(SummaryStatisticsW o1, SummaryStatisticsW o2) {
				return o1.getSum() > o2.getSum() ? 1 : (o1.getSum() == o2.getSum() ? 0 : -1);
			}
		});

		// total for all objects
		SummaryStatisticsW all = new SummaryStatisticsW();
		all.ref = "Total";
		for (SummaryStatistics item: sorted) {
			all.addValue(item.getSum());
		}
		sorted.add(all);
		Collections.reverse(sorted);

		// print values
		str.append(String.format("%-30s %11s %8s\n", "Object", "Time", "% Time"));
		for (SummaryStatisticsW item: sorted) {
			double perc = item.getSum() / all.getSum() * 100;
			double time = item.getSum() / 1000000000.0;
			str.append(String.format("%-30s %8.9f %8.1f\n", toStringChop(item.ref, 30), time, perc));
		}
		return str.toString();
	}

	public String dumpGrouped() {
		StringBuilder str = new StringBuilder();
		// make sure all stats are computed
		for (Object key: graph.getNodesMap().keySet()) {
			compute(key);
		}

		// grouped results
		HashMap<String, SummaryStatisticsW> statsByTaskBasename = new HashMap<String, SummaryStatisticsW>();
		for (Object key: graph.getNodesMap().keySet()) {
			if (key instanceof Task) {
				Task task = (Task) key;
				String name = new File(task.getName()).getName();
				SummaryStatisticsW local = statsByTaskBasename.get(name);
				if (local == null) {
					local = new SummaryStatisticsW();
					local.ref = name;
					statsByTaskBasename.put(name, local);
				}
				SummaryStatisticsW val = statsTotal.get(key);
				local.addValue(val.getSum());
			}
		}

		// sort statistics according to sum
		List<SummaryStatisticsW> sorted = new ArrayList<SummaryStatisticsW>(statsByTaskBasename.values());
		Collections.sort(sorted, new Comparator<SummaryStatisticsW>() {
			@Override
			public int compare(SummaryStatisticsW o1, SummaryStatisticsW o2) {
				return o1.getSum() > o2.getSum() ? 1 : (o1.getSum() == o2.getSum() ? 0 : -1);
			}
		});

		// total for all objects
		SummaryStatisticsW all = new SummaryStatisticsW();
		all.ref = "Total";
		for (SummaryStatistics item: sorted) {
			all.addValue(item.getSum());
		}
		sorted.add(all);
		Collections.reverse(sorted);

		// print values
		str.append(String.format("%-30s %11s %8s\n", "Object", "Time", "% Time"));
		for (SummaryStatisticsW item: sorted) {
			double perc = item.getSum() / all.getSum() * 100;
			double time = item.getSum() / 1000000000.0;
			str.append(String.format("%-30s %8.9f %8.1f\n", toStringChop(item.ref, 30), time, perc));
		}
		return str.toString();
	}

	private static String toStringChop(Object obj, int max) {
		String s = obj.toString();
		if (s.length() > max) {
			s = s.substring(0, max - 3) + "...";
		}
		return s;
	}

}
