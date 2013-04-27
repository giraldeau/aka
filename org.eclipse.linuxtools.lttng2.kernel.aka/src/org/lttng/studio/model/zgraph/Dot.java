package org.lttng.studio.model.zgraph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;

import com.google.common.collect.ArrayListMultimap;

public class Dot {

	private interface LabelProvider {
		public String nodeLabel(Node node);
		public String linkLabel(Node n0, Node n1, Link link);
	}

	private static class VerboseLabelProvider implements LabelProvider {
		private static final String fmtNode = "    %d [ label=\"[%d,%d]\" ];\n"; 		// id, id, timestamps
		private static final String fmtLink = "    %d -> %d [ label=\"%s,%d\" ];\n"; 	// id, id, type, duration
		@Override
		public String nodeLabel(Node node) {
			return String.format(fmtNode, node.getID(), node.getID(), node.getTs());
		}
		@Override
		public String linkLabel(Node n0, Node n1, Link link) {
			return String.format(fmtLink, n0.getID(), n1.getID(), link.type, link.duration());
		}
	}
	public static LabelProvider verbose = new VerboseLabelProvider();

	private static class PrettyLabelProvider implements LabelProvider {
		private static final String fmtNode = "    %d [ shape=box label=\"%d\" ]; // %s\n"; 		// id, id, timestamps
		private static final String fmtLink = "    %d -> %d [ label=\" %s, %.1f \" ];\n"; 	// id, id, type, duration
		private static final String fmtLinkRelax = "    %d -> %d [ label=\" %s, %.1f \" constraint=false ];\n"; 	// id, id, type, duration
		@Override
		public String nodeLabel(Node node) {
			if (node.numberOfNeighbor() == 0)
				return "";
			TmfTimestamp ts = new TmfTimestamp(node.getTs(), ITmfTimestamp.NANOSECOND_SCALE);
			return String.format(fmtNode, node.getID(), node.getID(), ts.toString());
		}
		@Override
		public String linkLabel(Node n0, Node n1, Link link) {
			boolean isVertical = n0.neighbor(Node.DOWN) == n1 || n1.neighbor(Node.UP) == n1;
			String fmt = fmtLink;
			if (isVertical) {
				fmt = fmtLinkRelax;
			}
			return String.format(fmt, n0.getID(), n1.getID(), link.type, link.duration() / 1000000.0);
		}
	}
	public static LabelProvider pretty = new PrettyLabelProvider();

	private static LabelProvider provider = verbose;

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
		if (g == null)
			return "";
		return todot(g, g.getNodesMap().keySet());
	}

	/**
	 * Generate dot string for provided objects
	 * @param g
	 * @param keys
	 * @return
	 */
	public static String todot(Graph g, Collection<? extends Object> keys) {
		if (g == null || keys == null)
			return "";
		int i = 0;
		StringBuilder str = new StringBuilder();
		str.append("/* ");
		str.append(g.toString());
		str.append(" */\n");
		str.append("digraph G {\n");
		//str.append("  rankdir=LR;\n");
		str.append("  overlap=false;");
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
				str.append(provider.linkLabel(n0, n1, lnk));
				neighbor.add(n);
			}
		}
		return neighbor;
	}

	private static void subgraph(StringBuilder str, Object obj, List<Node> list, int i) {
		str.append(String.format("  subgraph \"cluster_%d\" {\n", i));
		str.append("    rank=same;\n");
		str.append(String.format(
				"    title%d [ label=\"%s\", shape=plaintext ];\n", i,
				obj.toString()));
		for (Node node: list) {
			str.append(provider.nodeLabel(node));
		}
		str.append("}\n");
	}

	public static void writeString(String folder, String fname, String content) {
		try {
			File dir = new File("results", folder);
			dir.mkdirs();
			File fout = new File(dir, fname);
			FileWriter fwriter = new FileWriter(fout);
			fwriter.write(content);
			fwriter.flush();
			fwriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void writeString(Class<? extends Object> writer, String fname, String content) {
		String folder = writer.getClass().getName();
		writeString(folder, fname, content);
	}

	public static void setLabelProvider(LabelProvider p) {
		provider = p;
	}

}
