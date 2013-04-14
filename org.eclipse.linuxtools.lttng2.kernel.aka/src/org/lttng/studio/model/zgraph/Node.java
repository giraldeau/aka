package org.lttng.studio.model.zgraph;

public class Node {

	private static long count = 0;

	public Link prev;
	public Link next;
	public Link in;
	public Link out;
	private final long ts;
	private final long id;

	public Node(long ts) {
		this.ts = ts;
		this.id = count++;
	}

	/**
	 * Returns the timestamps of this node
	 * @return
	 */
	public long getTs() {
		return ts;
	}

	/**
	 * Returns the unique ID of this node
	 * @return
	 */
	public long getID() {
		return id;
	}

	/**
	 * Set PREV and NEXT pointers
	 * @param tail
	 * @param node
	 * @return
	 */
	public Link linkHorizontal(Node node) {
		Link link = new Link(this, node);
		this.next = link;
		node.prev = link;
		return link;
	}

	/**
	 * Set OUT and IN pointers
	 * @param from
	 * @param to
	 * @return
	 */
	public Link linkVertical(Node to) {
		Link link = new Link(this, to);
		this.out = link;
		to.in = link;
		return link;
	}

}
