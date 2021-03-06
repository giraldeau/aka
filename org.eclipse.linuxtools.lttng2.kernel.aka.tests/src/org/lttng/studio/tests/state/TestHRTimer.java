package org.lttng.studio.tests.state;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;
import org.lttng.studio.model.kernel.HRTimer;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.tests.basic.TestUtils;

public class TestHRTimer {

	@Test
	public void testHRTimerBasic() throws InterruptedException {
		String name = "sleep-1x-1sec-k";
		AnalyzerThread thread = TestUtils.setupAnalysis(name);
		thread.start();
		thread.join();
		
		SystemModel model = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
		HashMap<Long, HRTimer> hrTimers = model.getHRTimers();
		assertTrue(hrTimers.size() > 0);
	}
}
