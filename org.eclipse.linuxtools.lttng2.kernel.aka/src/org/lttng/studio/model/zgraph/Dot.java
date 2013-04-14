package org.lttng.studio.model.zgraph;

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
		int i = 0;
		StringBuilder str = new StringBuilder();
		str.append("digraph G {\n");
		str.append("  rankdir=LR;\n");
		ArrayListMultimap<Object, Node> map = g.getNodesMap();
		for (Object obj : map.keySet()) {
			str.append(String.format("  subgraph \"cluster_%d\" {\n", i));
			str.append("    rankdir=LR\n");
			str.append(String.format(
					"    title%d [ label=\"%s\", shape=plaintext ];\n", i,
					obj.toString()));
			List<Node> list = map.get(obj);
			for (Node node: list) {
				str.append(String.format(fmtNode, node.getID(), node.getID()));
			}
			i++;
			str.append("}\n");
			for (Node node: list) {
				if (node.next != null) {
					str.append(String.format(fmtLink, node.getID(), node.next.to.getID(), node.next.type));
				}
				if (node.out != null) {
					str.append(String.format(fmtLink, node.getID(), node.out.to.getID(), node.out.type));
				}
			}
		}
		str.append("}\n");
		return str.toString();
	}

}
