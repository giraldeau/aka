package org.lttng.studio.tests.basic;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.junit.Test;
import org.lttng.studio.reader.AnalysisPhase;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.TimeLoadingListener;
import org.lttng.studio.reader.handler.ITraceEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;

public class TestTimeLoadingProgress {

	int max = 0;
	ArrayList<Integer> res;
	@Test
	public void testTimeListener() throws Exception {
		res = new ArrayList<Integer>();
		TimeLoadingListener listener = new TimeLoadingListener("default", 2, new IProgressMonitor() {

			@Override
			public void beginTask(String name, int totalWork) {
				max = totalWork;
			}

			@Override
			public void done() {
				// TODO Auto-generated method stub

			}

			@Override
			public void internalWorked(double work) {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean isCanceled() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void setCanceled(boolean value) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setTaskName(String name) {
				// TODO Auto-generated method stub

			}

			@Override
			public void subTask(String name) {
				// TODO Auto-generated method stub

			}

			@Override
			public void worked(int work) {
				res.add(work);
			}

		});

		File trace = TestTraceset.getKernelTrace("netcat-tcp-k");

		AnalyzerThread thread = new AnalyzerThread();
		CtfTmfTrace ctfTmfTrace = new CtfTmfTrace();
		ctfTmfTrace.initTrace(null, trace.getCanonicalPath(), CtfTmfEvent.class);

		Collection<ITraceEventHandler> basic = TraceEventHandlerFactory.makeBasic();

		thread.setTrace(ctfTmfTrace);
		thread.addPhase(new AnalysisPhase(1, "test", basic));
		thread.addPhase(new AnalysisPhase(2, "test", basic));
		thread.setListener(listener);
		thread.start();
		thread.join();

		assertTrue(res.size() > 100);
		assertTrue(res.size() < max);
		assertTrue(Collections.max(res) <= 1000);
	}

}
