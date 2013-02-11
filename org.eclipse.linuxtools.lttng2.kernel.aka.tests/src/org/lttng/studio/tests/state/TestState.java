package org.lttng.studio.tests.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.linuxtools.lttng2.kernel.aka.JobListener;
import org.eclipse.linuxtools.lttng2.kernel.aka.JobManager;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.junit.Test;
import org.lttng.studio.model.kernel.FD;
import org.lttng.studio.model.kernel.FDSet;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.kernel.TaskBlockings;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.reader.handler.StatedumpEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerFD;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;
import org.lttng.studio.reader.handler.TraceEventHandlerSched;
import org.lttng.studio.tests.basic.TestTraceset;
import org.lttng.studio.utils.AnalysisFilter;

public class TestState {

	@Test
	public void testStatedumpTask() throws Exception {
		File traceDir = TestTraceset.getKernelTrace("burnP6-1x-1sec-k");
		TraceReader reader = new TraceReader();
		reader.setTrace(traceDir);
		StatedumpEventHandler handler = new StatedumpEventHandler();
		reader.register(handler);
		reader.process();
		SystemModel system = reader.getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
		Collection<Task> tasks = system.getTasks();
		assertTrue(tasks.size() > 0);
	}

	@Test
	public void testStatedumpFDs() throws Exception {
		File traceDir = TestTraceset.getKernelTrace("burnP6-1x-1sec-k");
		TraceReader reader = new TraceReader();
		reader.setTrace(traceDir);
		StatedumpEventHandler handler = new StatedumpEventHandler();
		reader.register(handler);
		reader.process();
		SystemModel system = reader.getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
		Collection<FD> fds = system.getFDs();
		assertTrue(fds.size() > 0);
	}

	@Test
	public void testStatedumpFDs2() throws Exception {
		File traceDir = TestTraceset.getKernelTrace("sleep-1x-1sec-k");
		TraceReader reader = new TraceReader();
		AnalysisFilter filter = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, AnalysisFilter.class);
		filter.addCommand(".*lttng.*");
		filter.setFollowChild(true);

		reader.setTrace(traceDir);
		reader.registerAll(TraceEventHandlerFactory.makeStatedump());
		reader.process();

		reader.clearHandlers();
		reader.registerAll(TraceEventHandlerFactory.makeInitialState());
		reader.process();

		reader.clearHandlers();
		reader.register(new TraceEventHandlerSched());
		reader.register(new TraceEventHandlerFD());
		reader.process();

		SystemModel system = reader.getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
		assertEquals(0, system.getDupUnkownFD());
	}

	public static void dumpFDs(SystemModel system, Task task) {
		System.out.println(task);
		FDSet fdSet = system.getFDSet(task);
		for (FD fd: fdSet.getFDs()) {
			System.out.println(fd);
		}
	}

	@Test
	public void testRetrieveCurrentTask() throws Exception {
		File traceDir = TestTraceset.getKernelTrace("burnP6-1x-1sec-k");
		TraceReader reader = new TraceReader();
		reader.setTrace(traceDir);
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
		reader.setTrace(traceDir);

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
		assertEquals(0, model2.getSwitchUnkowntask());
	}

	@Test
	public void testAnalysis1() throws IOException, InterruptedException, TmfTraceException {
		File traceDir = TestTraceset.getKernelTrace("sleep-1x-1sec-k");
		int i;
		int max = 100;
		ArrayList<Job> jobs = new ArrayList<Job>();
		for (i = 0; i < max; i++) {
			CtfTmfTrace ctfTmfTrace = new CtfTmfTrace();
			ctfTmfTrace.initTrace(null, traceDir.getCanonicalPath(), CtfTmfEvent.class);

			JobManager.getInstance().addListener(new JobListener() {
				@Override
				public void ready(ITmfTrace experiment) {
					ModelRegistry registry = JobManager.getInstance().getRegistry(experiment);
					TaskBlockings model = registry.getModel(IModelKeys.SHARED, TaskBlockings.class);
					//System.out.println(model.getEntries());
				}
			});
			Job job = JobManager.getInstance().launch(ctfTmfTrace);
			jobs.add(job);
		}
		for (i = 0; i < max; i++) {
			jobs.get(i).join();
		}
	}
}
