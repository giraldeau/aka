package org.lttng.studio.model.kernel;

import org.lttng.studio.reader.TraceReader;

public interface ITraceModel {

	public void reset();
	public void init(TraceReader reader);

}
