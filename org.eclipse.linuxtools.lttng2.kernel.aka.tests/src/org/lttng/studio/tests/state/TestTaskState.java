package org.lttng.studio.tests.state;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.Arrays;

import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.junit.Test;
import org.lttng.studio.model.kernel.EventCounter;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;
import org.lttng.studio.tests.basic.TestTraceset;

public class TestTaskState {

	@Test
	public void testHandleState() throws Exception {
		boolean failed = false;
		String[] kernelTraceset = TestTraceset.getKernelTraceset();
		Arrays.sort(kernelTraceset);
		for (String name: kernelTraceset) {
			long t0 = System.currentTimeMillis();
			File traceDir = TestTraceset.getKernelTrace(name);
			AnalyzerThread thread = new AnalyzerThread();
			thread.addAllPhases(TraceEventHandlerFactory.makeStandardAnalysis());
			thread.setTrace(traceDir);
			thread.start();
			thread.join();

			SystemModel model = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
			TmfTimeRange timeRange = thread.getReader().getTimeRange();
			long traceDuration = timeRange.getEndTime().getValue() - timeRange.getStartTime().getValue();
			long ts = System.currentTimeMillis() - t0;
			long ev = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, EventCounter.class).getCounter();
			double evProcessingRate = ((double) ev) / ts;
			double evProductionRate = ((double) ev * 1000000) / traceDuration;
			boolean ok = evProcessingRate > evProductionRate;
			String stat = String.format("[%5d %10.3f %10.1f %10.1f %3s]", ev / 1000, ts / 1000.0, evProductionRate, evProcessingRate, ok ? "OK" : "BAD");
			if (model.getSwitchUnkowntask() > 0) {
				System.err.println("FAIL: " + stat + " " + name);
				failed = true;
			} else {
				System.out.println("PASS: " + stat + " " + name);
			}
		}
		System.out.println("DONE");
		assertFalse(failed);
	}

}
