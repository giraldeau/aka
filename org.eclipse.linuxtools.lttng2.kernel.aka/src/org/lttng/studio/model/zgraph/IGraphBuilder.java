package org.lttng.studio.model.zgraph;

public interface IGraphBuilder {

	public void build(GraphBuilderData data);
	public void criticalPathBounded(GraphBuilderData data);
	public void criticalPathUnbounded(GraphBuilderData data);
	public GraphBuilderData[] params();
}
