package org.lttng.studio.model.zgraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;


public class Dot {

	private static final String fmtNode = "    %d [ label=\"[%d]\" ];\n"; 		// id, id
	private static final String fmtLink = "    %d -> %d [ label=\"%s\" ];\n"; 	// id, id, type

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
		for (Object obj : keys) {
			List<Node> list = g.getNodesOf(obj);
			subgraph(str, obj, list, i);
			i++;
			for (Node node: list) {
				if (node.next != null) {
					str.append(String.format(fmtLink, node.getID(), node.next.to.getID(), node.next.type));
				}
				if (node.out != null) {
					str.append(String.format(fmtLink, node.getID(), node.out.to.getID(), node.out.type));
					Object o = g.getParentOf(node.out.to);
					if (!set.contains(o)) {
						extra.put(o, node.out.to);
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

	private static void subgraph(StringBuilder str, Object obj, List<Node> list, int i) {
		str.append(String.format("  subgraph \"cluster_%d\" {\n", i));
		str.append("    rankdir=LR\n");
		str.append(String.format(
				"    title%d [ label=\"%s\", shape=plaintext ];\n", i,
				obj.toString()));
		for (Node node: list) {
			str.append(String.format(fmtNode, node.getID(), node.getID()));
		}
		str.append("}\n");
	}

}
