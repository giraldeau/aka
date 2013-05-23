package org.lttng.studio.model.zgraph.analysis;

import org.lttng.studio.model.zgraph.Graph;
import org.lttng.studio.model.zgraph.Node;

public interface ICriticalPathAlgorithm {
	public Graph compute(Node start, Node end);
}
