package org.lttng.studio.model.zgraph;

public class Node implements Comparable<Node> {

	private static long count = 0;

	public static final int UP = 0;
	public static final int DOWN = 1;
	public static final int RIGHT = 2;
	public static final int LEFT = 3;

	public Link[] links;
	private long ts;
	private final long id;

	public Node() {
		this(0);
	}

	public Node(long ts) {
		links = new Link[4];
		this.ts = ts;
		this.id = count++;
	}

	public Node(Node node) {
		this();
		this.ts = node.ts;
	}

	/**
	 * Returns the timestamps of this node
	 * @return
	 */
	public long getTs() {
		return ts;
	}

	/**
	 * Set the timestamps of this node
	 * @return
	 */
	public void setTs(long ts) {
		this.ts = ts;
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
	public Link linkHorizontal(Node to) {
		checkTimestamps(to);
		return linkHorizontalRaw(to);
	}

	/**
	 * Set UP and DOWN pointers
	 * @param from
	 * @param to
	 * @return
	 */
	public Link linkVertical(Node to) {
		checkTimestamps(to);
		return linkVerticalRaw(to);
	}

	public Link linkVerticalRaw(Node to) {
		Link link = new Link(this, to);
		this.links[UP] = link;
		to.links[DOWN] = link;
		return link;
	}

	public Link linkHorizontalRaw(Node node) {
		Link link = new Link(this, node);
		this.links[RIGHT] = link;
		node.links[LEFT] = link;
		return link;
	}

	private void checkTimestamps(Node to) {
		if (this.ts > to.ts)
			throw new IllegalArgumentException("Next node timestamps must be " +
					"greater or equal to current timestamps: " +
					String.format("(curr=%d,next=%d,elapsed=%d)", ts, to.ts, to.ts - ts));
	}

	public Node neighbor(int dir) {
		switch(dir) {
		case UP:
			return up();
		case DOWN:
			return down();
		case RIGHT:
			return right();
		case LEFT:
			return left();
		default:
			break;
		}
		return null;
	}

	public Node up() {
		if (links[UP] != null)
			return links[UP].to;
		return null;
	}

	public Node down() {
		if (links[DOWN] != null)
			return links[DOWN].from;
		return null;
	}

	public Node right() {
		if (links[RIGHT] != null)
			return links[RIGHT].to;
		return null;
	}

	public Node left() {
		if (links[LEFT] != null)
			return links[LEFT].from;
		return null;
	}

	@Override
	public int compareTo(Node other) {
		return this.ts > other.ts ? 1 : (this.ts == other.ts ? 0 : -1);
	}

	@Override
	public String toString() {
		return "[" + id + "," + ts + "]";
	}

}
