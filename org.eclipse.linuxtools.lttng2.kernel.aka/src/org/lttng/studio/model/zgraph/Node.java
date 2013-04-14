package org.lttng.studio.model.zgraph;

public class Node {

	public Link prev;
	public Link next;
	public Link in;
	public Link out;
	private final long ts;

	public Node(long ts) {
		this.ts = ts;
	}

	public long getTs() {
		return ts;
	}

}
