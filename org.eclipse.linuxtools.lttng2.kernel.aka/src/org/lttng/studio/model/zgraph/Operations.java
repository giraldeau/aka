package org.lttng.studio.model.zgraph;

import java.util.List;

import org.lttng.studio.collect.BinarySearch;

public class Operations {

	public static Graph criticalPath(Graph g, Object obj) {
		return criticalPath(g, obj, g.getHead(obj).getTs(), g.getTail(obj).getTs());
	}

	public static Graph criticalPath(Graph g, Object obj, long start, long end) {
		Graph path = new Graph();
		List<Node> main = g.getNodesOf(obj);
		// FIXME: handle nodes with equal timestamps
		int floor = BinarySearch.floor(main, new Node(start));
		int ceiling = BinarySearch.ceiling(main, new Node(end));
		for (int i = floor; i <= ceiling; i++) {
			Node curr = main.get(i);
			Link link = path.append(g.getParentOf(curr), new Node(curr.getTs()));
			if (link != null)
				link.type = curr.links[Node.R].type;
		}
		return path;
	}

}
