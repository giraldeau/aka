package org.lttng.studio.model.zgraph;

public abstract class GraphBuilder implements IGraphBuilder {
	private final String name;
	public GraphBuilder(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
}
