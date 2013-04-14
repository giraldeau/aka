package org.lttng.studio.model.zgraph;

import java.util.List;

import com.google.common.collect.ArrayListMultimap;

public class Graph {

	private final ArrayListMultimap<Object, Node> nodeMap;

	public Graph() {
		nodeMap = ArrayListMultimap.create();
	}

	/**
	 * Replace tail node of the provided object without linking.
	 * @param obj
	 * @param node
	 */
	public void put(Object obj, Node node) {
		List<Node> list = nodeMap.get(obj);
		if (!list.isEmpty()) {
			list.remove(list.size() - 1);
		}
		list.add(node);
	}

	/**
	 * Add node to object's list and make horizontal link with tail.
	 * @param obj
	 * @param node
	 * @return
	 */
	public Link append(Object obj, Node node) {
		List<Node> list = nodeMap.get(obj);
		Node tail = getTail(obj);
		Link link = null;
		if (tail != null) {
			if (tail.getTs() > node.getTs()) {
				throw new IllegalArgumentException("node must be ordered by ts");
			}
			link = linkHorizontal(tail, node);
		}
		list.add(node);
		return link;
	}

	/**
	 * Set PREV and NEXT pointers
	 * @param tail
	 * @param node
	 * @return
	 */
	public Link linkHorizontal(Node tail, Node node) {
		Link link = new Link(tail, node);
		tail.next = link;
		node.prev = link;
		return link;
	}

	/**
	 * Set FROM and TO pointers
	 * @param from
	 * @param to
	 * @return
	 */
	public Link linkVertical(Node from, Node to) {
		Link link = new Link(from, to);
		from.out = link;
		to.in = link;
		return link;
	}

	/**
	 * Returns tail node of the provided object
	 * @param obj
	 * @return
	 */
	public Node getTail(Object obj) {
		List<Node> list = nodeMap.get(obj);
		if (!list.isEmpty()) {
			return list.get(list.size() - 1);
		}
		return null;
	}

	/**
	 * Returns all nodes of the provided object.
	 * @param obj
	 * @return
	 */
	public List<Node> getNodesOf(Object obj) {
		return nodeMap.get(obj);
	}

}
