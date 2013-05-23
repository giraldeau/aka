package org.lttng.studio.model.zgraph.analysis;

import org.lttng.studio.model.zgraph.Graph;
import org.lttng.studio.model.zgraph.LinkType;
import org.lttng.studio.model.zgraph.Node;

public abstract class AbstractCriticalPathAlgorithm implements ICriticalPathAlgorithm {

	private final Graph graph;

	public AbstractCriticalPathAlgorithm(Graph graph) {
		this.graph = graph;
	}

	public Graph getGraph() {
		return graph;
	}

	/**
	 * Copy link of type TYPE between nodes FROM and TO in the graph PATH.
	 * The return value is the tail node for the new path.
	 * @param path
	 * @param anchor
	 * @param from
	 * @param to
	 * @param type
	 * @return
	 */
	public Node copyLink(Graph path, Node anchor, Node from, Node to, long ts, LinkType type) {
		Object parentFrom = getGraph().getParentOf(from);
		Object parentTo = getGraph().getParentOf(to);
		Node tmp = new Node(ts);
		path.add(parentTo, tmp);
		if (parentFrom == parentTo) {
			anchor.linkHorizontal(tmp).type = type;
		} else {
			anchor.linkVertical(tmp).type = type;
		}
		return tmp;
	}

	public static Node findIncoming(Node node, int dir) {
		while(true) {
			if (node.hasNeighbor(Node.DOWN))
				return node;
			if (!(node.hasNeighbor(dir) &&
					node.links[dir].type == LinkType.EPS)) {
				break;
			}
			node = node.neighbor(dir);
		}
		return null;
	}

}
