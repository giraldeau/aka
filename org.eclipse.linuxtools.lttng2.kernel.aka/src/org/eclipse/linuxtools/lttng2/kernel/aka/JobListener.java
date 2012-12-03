package org.eclipse.linuxtools.lttng2.kernel.aka;

import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

public interface JobListener {

	public void ready(TmfExperiment<?> experiment);

}
