package org.eclipse.linuxtools.lttng2.kernel.aka;

import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

public interface JobListener {

	public void ready(ITmfTrace experiment);

}
