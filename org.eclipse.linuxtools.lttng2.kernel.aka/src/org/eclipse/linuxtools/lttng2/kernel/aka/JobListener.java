package org.eclipse.linuxtools.lttng2.kernel.aka;

import org.lttng.studio.reader.AnalyzerThread;

public interface JobListener {

	public void ready(AnalyzerThread thread);

}
