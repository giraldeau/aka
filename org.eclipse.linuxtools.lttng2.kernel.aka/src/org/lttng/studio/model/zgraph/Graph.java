package org.lttng.studio.model.zgraph;

import java.util.HashMap;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;

public class Graph {

	private final ArrayListMultimap<Object, Node> nodeMap;
	private final HashMap<Node, Object> reverse;

	public Graph() {
		nodeMap = ArrayListMultimap.create();
		reverse = new HashMap<Node, Object>();
	}

	/**
	 * Replace tail node of the provided object without linking.
	 * @param obj
	 * @param node
	 */
	public void replace(Object obj, Node node) {
		List<Node> list = nodeMap.get(obj);
		if (!list.isEmpty()) {
			Node n = list.remove(list.size() - 1);
			reverse.remove(n);
		}
		list.add(node);
		reverse.put(node, obj);
	}

	/**
	 * Add node to the provided object without linking
	 * @param obj
	 * @param node
	 */
	public void add(Object obj, Node node) {
		List<Node> list = nodeMap.get(obj);
		list.add(node);
		reverse.put(node, obj);
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
			link = tail.linkHorizontal(node);
		}
		list.add(node);
		reverse.put(node, obj);
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
	 * Returns head node of the provided object
	 * @param obj
	 * @return
	 */
	public Node getHead(Object obj) {
		List<Node> list = nodeMap.get(obj);
		if (!list.isEmpty()) {
			return list.get(0);
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

	public Object getParentOf(Node node) {
		return reverse.get(node);
	}

	public ArrayListMultimap<Object, Node> getNodesMap() {
		return nodeMap;
	}

	public int size() {
		int size = 0;
		for (Node node: reverse.keySet()) {
			size++;
			for (int i = 0; i < node.links.length; i++) {
				if (node.links[i] != null)
					size++;
			}
		}
		return size;
	}

}
