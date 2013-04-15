package org.lttng.studio.model.zgraph;

public class Node {

	private static long count = 0;

	public static final int U = 0;
	public static final int D = 1;
	public static final int R = 2;
	public static final int L = 3;

	public Link[] links;
	private final long ts;
	private final long id;

	public Node(long ts) {
		links = new Link[4];
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
	 * Set RIGHT and LEFT pointers
	 * @param tail
	 * @param node
	 * @return
	 */
	public Link linkHorizontal(Node node) {
		Link link = new Link(this, node);
		this.links[R] = link;
		node.links[L] = link;
		return link;
	}

	/**
	 * Set UP and DOWN pointers
	 * @param from
	 * @param to
	 * @return
	 */
	public Link linkVertical(Node to) {
		Link link = new Link(this, to);
		this.links[U] = link;
		to.links[D] = link;
		return link;
	}

	public Node neighbor(int dir) {
		switch(dir) {
		case U:
			return U();
		case D:
			return D();
		case R:
			return R();
		case L:
			return L();
		default:
			break;
		}
		return null;
	}

	public Node U() {
		if (links[U] != null)
			return links[U].to;
		return null;
	}

	public Node D() {
		if (links[D] != null)
			return links[D].from;
		return null;
	}

	public Node R() {
		if (links[R] != null)
			return links[R].to;
		return null;
	}

	public Node L() {
		if (links[L] != null)
			return links[L].from;
		return null;
	}

}
