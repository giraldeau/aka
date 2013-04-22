package org.lttng.studio.model.zgraph;

public interface IGraphBuilder {

	public void build(GraphBuilderData data);
	public void criticalPath(GraphBuilderData data);
	public GraphBuilderData getDefaults();
}
