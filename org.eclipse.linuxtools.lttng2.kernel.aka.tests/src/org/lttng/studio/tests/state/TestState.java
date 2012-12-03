package org.lttng.studio.tests.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;

import org.junit.Test;
import org.lttng.studio.model.kernel.FD;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.reader.handler.StatedumpEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerFD;
import org.lttng.studio.reader.handler.TraceEventHandlerSched;
import org.lttng.studio.tests.basic.TestTraceset;

public class TestState {

	//@Test
	public void testStatedumpTask() throws Exception {
		File traceDir = TestTraceset.getKernelTrace("burnP6-1x-1sec-k");
		TraceReader reader = new TraceReader();
		reader.addTrace(traceDir);
		StatedumpEventHandler handler = new StatedumpEventHandler();
		reader.register(handler);
		reader.process();
		SystemModel system = reader.getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
		Collection<Task> tasks = system.getTasks();
		assertTrue(tasks.size() > 0);
	}

	//@Test
	public void testStatedumpFDs() throws Exception {
		File traceDir = TestTraceset.getKernelTrace("burnP6-1x-1sec-k");
		TraceReader reader = new TraceReader();
		reader.addTrace(traceDir);
		StatedumpEventHandler handler = new StatedumpEventHandler();
		reader.register(handler);
		reader.process();
		SystemModel system = reader.getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
		Collection<FD> fds = system.getFDs();
		assertTrue(fds.size() > 0);
	}

	//@Test
	public void testRetrieveCurrentTask() throws Exception {
		File traceDir = TestTraceset.getKernelTrace("burnP6-1x-1sec-k");
		TraceReader reader = new TraceReader();
		reader.addTrace(traceDir);
		TraceEventHandlerSched handler = new TraceEventHandlerSched();
		reader.register(handler);
		reader.process();
		SystemModel model = reader.getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
		assertTrue(model.getCurrentTid(0) >= 0);
	}

	@Test
	public void testHandleOpenCloseFDs() throws Exception {
		File traceDir = TestTraceset.getKernelTrace("burnP6-1x-1sec-k");
		TraceReader reader = new TraceReader();
		reader.addTrace(traceDir);

		SystemModel model1 = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		// Phase 1: build initial state
		StatedumpEventHandler h0 = new StatedumpEventHandler();
		reader.register(h0);
		reader.process();
		reader.clearHandlers();

		// Phase 2: update current state
		TraceEventHandlerSched h1 = new TraceEventHandlerSched();
		TraceEventHandlerFD h2 = new TraceEventHandlerFD();
		reader.register(h1);
		reader.register(h2);
		reader.process();

		SystemModel model2 = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		assertSame(model1, model2);
		assertEquals(0, h1.getSchedSwitchUnkownTask());
	}

}
