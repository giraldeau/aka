package org.lttng.studio.model.graph;

import org.jgrapht.graph.DefaultWeightedEdge;

public class ExecEdge extends DefaultWeightedEdge {
	private static final long serialVersionUID = -5209049262649453792L;
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

	public double getWeight() {
		return weight;
	}

}
