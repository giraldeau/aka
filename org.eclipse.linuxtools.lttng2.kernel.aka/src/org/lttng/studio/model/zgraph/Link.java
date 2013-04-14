package org.lttng.studio.model.zgraph;

public class Link {

	public LinkType type;
	public Node from;
	public Node to;

	public Link(Node from, Node to) {
		this.from = from;
		this.to = to;
		this.type = LinkType.DEFAULT;
	}
}
