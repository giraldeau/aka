package org.lttng.studio.tests.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.kernel.TaskBlockingEntry;
import org.lttng.studio.model.kernel.TaskBlockings;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.reader.handler.StatedumpEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerBlocking;
import org.lttng.studio.reader.handler.TraceEventHandlerSched;
import org.lttng.studio.tests.basic.TestTraceset;

public class TestTaskBlocking {

	public static long NANO = 1000000000;

	@Test
	public void testTaskBlocking() throws Exception {
		File traceDir = TestTraceset.getKernelTrace("sleep-1x-1sec-k");
		TraceReader reader = new TraceReader();
		reader.setTrace(traceDir);

		// Phase 1: build initial state
		StatedumpEventHandler h0 = new StatedumpEventHandler();
		reader.register(h0);
		reader.process();
		reader.clearHandlers();

		// Phase 2: update current state, compute blocking
		TraceEventHandlerBlocking h2 = new TraceEventHandlerBlocking();
		TraceEventHandlerSched h1 = new TraceEventHandlerSched();
		reader.register(h1);
		reader.register(h2);
		reader.process();

		TaskBlockingEntry sleep = null;
		TaskBlockings blockings = reader.getRegistry().getModel(IModelKeys.SHARED, TaskBlockings.class);

		for (Task task: blockings.getEntries().keySet()) {
			if (task.getName().endsWith("sleep")) {
				List<TaskBlockingEntry> entries = blockings.getEntries().get(task);
				for (TaskBlockingEntry entry: entries) {
					if (entry.getSyscall().getEventName().equals("sys_nanosleep")) {
						sleep = entry;
						break;
					}
				}
				break;
			}
		}
		System.out.println(sleep + " " + ((double)sleep.getInterval().duration()) / NANO);
		assertNotNull(sleep);
		assertEquals(1.0, sleep.getInterval().duration() / NANO, 0.01 );
	}

}
