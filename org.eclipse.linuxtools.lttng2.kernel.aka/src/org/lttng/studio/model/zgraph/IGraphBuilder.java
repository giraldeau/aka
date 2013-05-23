package org.lttng.studio.model.zgraph;

import org.lttng.studio.model.zgraph.analysis.CriticalPathAlgorithmBounded;
import org.lttng.studio.model.zgraph.analysis.CriticalPathAlgorithmUnbounded;


public interface IGraphBuilder {

	public void buildGraph(GraphBuilderData data);
	public void criticalPath(GraphBuilderData data, CriticalPathAlgorithmBounded klass);
	public void criticalPath(GraphBuilderData data, CriticalPathAlgorithmUnbounded klass);
	public GraphBuilderData[] params();
}
