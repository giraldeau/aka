package org.lttng.studio.model.zgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;

public class Dot {

	private static final String fmtNode = "    %d [ label=\"[%d,%d]\" ];\n"; 		// id, id, timestamps
	private static final String fmtLink = "    %d -> %d [ label=\"%s,%d\" ];\n"; 	// id, id, type, duration

	/**
	 * Generate dot string from head node
	 * @param node
	 * return
	 */
	public static String todot(Node node) {
		Graph g = Ops.toGraph(node);
		return todot(g);
	}

	/**
	 * Generate dot string for the complete graph, grouped by objects
	 * @param g
	 * @return
	 */
	public static String todot(Graph g) {
		return todot(g, g.getNodesMap().keySet());
	}

	/**
	 * Generate dot string for provided objects
	 * @param g
	 * @param keys
	 * @return
	 */
	public static String todot(Graph g, Collection<Object> keys) {
		int i = 0;
		StringBuilder str = new StringBuilder();
		str.append("digraph G {\n");
		str.append("  rankdir=LR;\n");
		ArrayListMultimap<Object, Node> extra = ArrayListMultimap.create();
		HashSet<Object> set = new HashSet<Object>();
		set.addAll(keys);
		HashSet<Node> visited = new HashSet<Node>();
		for (Object obj : keys) {
			List<Node> list = g.getNodesOf(obj);
			subgraph(str, obj, list, i);
			i++;
			for (Node node: list) {
				List<Node> neighbors = visit(str, visited, node);
				for (Node n: neighbors) {
					Object o = g.getParentOf(n);
					if (!set.contains(o)) {
						extra.put(o, n);
					}
				}
			}
		}
		for (Object obj: extra.keySet()) {
			List<Node> list = extra.get(obj);
			subgraph(str, obj, list, i);
			i++;
		}
		str.append("}\n");
		return str.toString();
	}

	private static List<Node> visit(StringBuilder str, Set<Node> visited, Node node) {
		List<Node> neighbor = new ArrayList<Node>();
		if (visited.contains(node))
			return neighbor;
		visited.add(node);
		for (int dir = 0; dir < node.links.length; dir++) {
			Node n = node.neighbor(dir);
			if (n == null)
				continue;
			Link lnk = node.links[dir];
			Node n0 = node;
			Node n1 = n;
			if (dir == Node.LEFT || dir == Node.DOWN) {
				n0 = n;
				n1 = node;
			}
			if (!visited.contains(n)) {
				str.append(String.format(fmtLink, n0.getID(), n1.getID(), lnk.type, lnk.duration()));
				neighbor.add(n);
			}
		}
		return neighbor;
	}

	private static void subgraph(StringBuilder str, Object obj, List<Node> list, int i) {
		str.append(String.format("  subgraph \"cluster_%d\" {\n", i));
		str.append("    rankdir=LR\n");
		str.append(String.format(
				"    title%d [ label=\"%s\", shape=plaintext ];\n", i,
				obj.toString()));
		for (Node node: list) {
			str.append(String.format(fmtNode, node.getID(), node.getID(), node.getTs()));
		}
		str.append("}\n");
	}

}
