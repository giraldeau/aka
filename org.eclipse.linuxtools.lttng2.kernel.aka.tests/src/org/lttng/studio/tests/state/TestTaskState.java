package org.lttng.studio.tests.state;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.Arrays;

import org.eclipse.linuxtools.tmf.core.event.TmfTimeRange;
import org.junit.Test;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.reader.handler.StatedumpEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerCounter;
import org.lttng.studio.reader.handler.TraceEventHandlerSched;
import org.lttng.studio.reader.handler.TraceEventHandlerSchedInit;
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
			TraceReader reader = new TraceReader();
			reader.setTrace(traceDir);

			// Phase 1: build initial state
			StatedumpEventHandler h0 = new StatedumpEventHandler();
			reader.register(h0);
			reader.process();
			reader.clearHandlers();

			// Phase 2: get scheduled process
			TraceEventHandlerSchedInit h1 = new TraceEventHandlerSchedInit();
			reader.register(h1);
			reader.process();
			reader.clearHandlers();

			TraceEventHandlerSched h2 = new TraceEventHandlerSched();
			TraceEventHandlerCounter h3 = new TraceEventHandlerCounter();
			reader.register(h2);
			reader.register(h3);
			reader.process();

			TmfTimeRange timeRange = reader.getTimeRange();
			long traceDuration = timeRange.getEndTime().getValue() - timeRange.getStartTime().getValue();
			long ts = System.currentTimeMillis() - t0;
			long ev = h3.getCounter();
			double evProcessingRate = ((double) ev) / ts;
			double evProductionRate = ((double) ev * 1000000) / traceDuration;
			boolean ok = evProcessingRate > evProductionRate;
			String stat = String.format("[%5d %10.3f %10.1f %10.1f %3s]", ev / 1000, ts / 1000.0, evProductionRate, evProcessingRate, ok ? "OK" : "BAD");
			if (h2.getSchedSwitchUnkownTask() > 0) {
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
