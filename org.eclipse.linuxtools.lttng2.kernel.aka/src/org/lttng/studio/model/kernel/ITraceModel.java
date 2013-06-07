package org.lttng.studio.model.kernel;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.lttng.studio.reader.TraceReader;

public interface ITraceModel {

	public void reset();
	public void init(TraceReader reader, CtfTmfTrace trace);

}
