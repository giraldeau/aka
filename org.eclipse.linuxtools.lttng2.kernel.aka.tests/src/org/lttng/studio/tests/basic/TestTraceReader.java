package org.lttng.studio.tests.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.junit.Test;
import org.lttng.studio.model.kernel.EventCounter;
import org.lttng.studio.reader.AnalysisPhase;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.reader.handler.ITraceEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerCounter;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;
import org.lttng.studio.utils.CTFUtils;

/**
 * Test simple trace reader
 * @author francis
 *
 */
public class TestTraceReader {

	@Test
	public void testSimpleTraceReaderLoad() throws Exception {
		File trace = TestTraceset.getKernelTrace("burnP6-1x-1sec-k");
		TraceReader reader = new TraceReader();
		reader.setMainTrace(trace);
		TraceEventHandlerCounter handler = new TraceEventHandlerCounter();
		reader.register(handler);
		reader.process();
		EventCounter counter = reader
				.getRegistry()
				.getModelForTrace((CtfTmfTrace)reader.getMainTrace(), EventCounter.class);
		assertTrue(counter.getCounter() > 0);
	}

	@Test
	public void testGetNbCpus() throws IOException, TmfTraceException {
		File trace = TestTraceset.getKernelTrace("burnP6-1x-1sec-k");
		// assume traces comes from 8 cores CPU
		int num = CTFUtils.getNumCpuFromCtfTrace(CTFUtils.makeCtfTrace(trace));
		assertEquals(8, num);
	}

	@Test
	public void testLoadMultipleTraces() throws Exception {
		EventCounter counter;
		TraceReader reader;
		File trace1 = TestTraceset.getKernelTrace("wk-heartbeat-k-u");
		File trace2 = TestTraceset.getUSTTrace("wk-heartbeat-k-u");
		TraceEventHandlerCounter handler = new TraceEventHandlerCounter();
		TmfExperiment experiment = CTFUtils.makeTmfExperiment(new File[] {trace1, trace2});

		// trace 1
		reader = new TraceReader();
		reader.register(handler);
		reader.setMainTrace(trace1);
		reader.process();
		counter = reader.getRegistry().getModelForTrace((CtfTmfTrace)reader.getMainTrace(), EventCounter.class);
		long cnt1 = counter.getCounter();

		// trace 2
		reader = new TraceReader();
		reader.register(handler);
		reader.setMainTrace(trace2);
		reader.process();
		counter = reader.getRegistry().getModelForTrace((CtfTmfTrace)reader.getMainTrace(), EventCounter.class);
		long cnt2 = counter.getCounter();

		// trace 1 and 2
		reader = new TraceReader();
		reader.register(handler);
		reader.setTrace(experiment);
		reader.process();
		counter = reader.getRegistry().getModelForTrace((CtfTmfTrace)reader.getMainTrace(), EventCounter.class);
		long cnt3 = counter.getCounter();

		assertEquals(cnt1 + cnt2, cnt3);
	}

	@Test
	public void testInvariant() throws IOException, TmfTraceException, InterruptedException {
		File trace = TestTraceset.getKernelTrace("sleep-1x-1sec-k");
		Collection<ITraceEventHandler> basic = TraceEventHandlerFactory.makeBasic();
		AnalyzerThread thread = new AnalyzerThread();
		thread.setTrace(trace);
		thread.addPhase(new AnalysisPhase(1, "test", basic));
		thread.start();
		thread.join();
		assertTrue(true);
	}

	@Test
	public void testStatedump() throws Exception {
		File trace = TestTraceset.getKernelTrace("sleep-1x-1sec-k");
		Collection<ITraceEventHandler> handlers = TraceEventHandlerFactory.makeStatedump();
		TraceReader reader = new TraceReader();
		reader.setMainTrace(trace);
		reader.registerAll(handlers);
		reader.process();
		assertTrue(true);
	}

	@Test
	public void testFull() throws Exception {
		File trace = TestTraceset.getKernelTrace("sleep-1x-1sec-k");
		Collection<ITraceEventHandler> handlers = TraceEventHandlerFactory.makeMain();
		TraceReader reader = new TraceReader();
		reader.setMainTrace(trace);
		reader.registerAll(handlers);
		reader.process();
		assertTrue(true);
	}

}
