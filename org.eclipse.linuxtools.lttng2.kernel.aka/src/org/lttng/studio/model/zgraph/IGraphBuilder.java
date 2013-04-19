package org.lttng.studio.model.zgraph;

public interface IGraphBuilder {

	public void build(GraphBuilderData state);
	public GraphBuilderData getDefaults();
}
