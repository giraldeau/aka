package org.lttng.studio.tests.state;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.HashMap;

import org.junit.Test;
import org.lttng.studio.model.kernel.BlockingItem;
import org.lttng.studio.model.kernel.ModelEvent;
import org.lttng.studio.model.kernel.ModelListener;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TraceReader;
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
		reader.addTrace(traceDir);

		final HashMap<Task, BlockingItem> blockings = new HashMap<Task, BlockingItem>();

		// Phase 1: build initial state
		StatedumpEventHandler h0 = new StatedumpEventHandler();
		reader.register(h0);
		reader.process();
		reader.clearHandlers();

		// Phase 2: update current state, compute blocking
		TraceEventHandlerBlocking h2 = new TraceEventHandlerBlocking();
		h2.addListener(ModelEvent.BLOCKING, new ModelListener() {
			@Override
			public void handleEvent(ModelEvent event) {
				switch(event.type) {
				case ModelEvent.BLOCKING:
					Task task = event.blocking.getTask();
					blockings.put(task, event.blocking);
					break;
				default:
					break;
				}
			}
		});
		TraceEventHandlerSched h1 = new TraceEventHandlerSched();
		reader.register(h1);
		reader.register(h2);
		reader.process();

		BlockingItem sleep = null;
		for (Task task: blockings.keySet()) {
			if (task.getName().endsWith("sleep")) {
				BlockingItem item = blockings.get(task);
				if (item.getSyscall().getDeclaration().getName().equals("sys_nanosleep")) {
					sleep = item;
					break;
				}
			}
		}
		assertNotNull(sleep);
		// FIXME: add interval support
		//assertEquals(1.0, sleep.getInterval().getDuration() / NANO, 0.01 );
	}

}
