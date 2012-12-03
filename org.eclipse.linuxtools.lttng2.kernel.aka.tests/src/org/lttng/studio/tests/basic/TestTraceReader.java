package org.lttng.studio.tests.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
import org.junit.Test;
import org.lttng.studio.model.kernel.EventCounter;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.reader.handler.TraceEventHandlerCounter;

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
		reader.addTrace(trace);
		TraceEventHandlerCounter handler = new TraceEventHandlerCounter();
		reader.register(handler);
		reader.process();
		EventCounter counter = reader.getRegistry().getModel(IModelKeys.SHARED, EventCounter.class);
		assertTrue(counter.getCounter() > 0);
	}

	@Test
	public void testGetNbCpus() throws IOException, CTFReaderException {
		File trace = TestTraceset.getKernelTrace("burnP6-1x-1sec-k");
		TraceReader reader = new TraceReader();
		reader.addTrace(trace);
		reader.loadTrace();
		// assume traces comes from 8 cores CPU
		assertEquals(8, reader.getNumCpus());
	}

	@Test
	public void testLoadMultipleTraces() throws Exception {
		EventCounter counter;
		TraceReader reader;
		File trace1 = TestTraceset.getKernelTrace("wk-heartbeat-k-u");
		File trace2 = TestTraceset.getUSTTrace("wk-heartbeat-k-u");
		TraceEventHandlerCounter handler = new TraceEventHandlerCounter();

		// trace 1
		reader = new TraceReader();
		reader.register(handler);
		reader.addTrace(trace1);
		reader.process();
		counter = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, EventCounter.class);
		long cnt1 = counter.getCounter();

		// trace 2
		reader = new TraceReader();
		reader.register(handler);
		reader.addTrace(trace2);
		reader.process();
		counter = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, EventCounter.class);
		long cnt2 = counter.getCounter();

		// trace 1 and 2
		reader = new TraceReader();
		reader.register(handler);
		reader.addTrace(trace1);
		reader.addTrace(trace2);
		reader.process();
		counter = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, EventCounter.class);
		long cnt3 = counter.getCounter();

		assertEquals(cnt1 + cnt2, cnt3);
	}

}
