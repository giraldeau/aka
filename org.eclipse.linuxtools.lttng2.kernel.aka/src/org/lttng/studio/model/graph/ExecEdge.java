package org.lttng.studio.model.graph;

public class ExecEdge {

	private EdgeType type;
	private final long weight;

	public ExecEdge(long weight) {
		this(weight, EdgeType.DEFAULT);
	}

	public ExecEdge(long weight, EdgeType type) {
		this.weight = weight;
		this.type = type;
	}

	public EdgeType getType() {
		return type;
	}

	public void setType(EdgeType type) {
		this.type = type;
	}

	public long getWeight() {
		return weight;
	}

}
