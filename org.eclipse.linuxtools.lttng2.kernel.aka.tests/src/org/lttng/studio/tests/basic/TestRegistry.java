package org.lttng.studio.tests.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
import org.eclipse.linuxtools.ctf.core.trace.CTFTrace;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfContext;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.junit.Before;
import org.junit.Test;
import org.lttng.studio.model.kernel.EventCounter;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.reader.Host;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.utils.CTFUtils;

import com.google.common.collect.Multimap;

public class TestRegistry {

	@Test
	public void testRecoverHost() throws IOException, CTFReaderException {
		String uuid = "df194be5-29ec-4143-9d8c-12be07862030";
		File traceDir = TestTraceset.getKernelTrace("netcat-udp-k");
		CTFTrace trace = new CTFTrace(traceDir);
		CTFUtils.setTraceClockUUID(trace, UUID.fromString(uuid));
		UUID act = CTFUtils.getTraceClockUUID(trace);
		UUID exp = UUID.fromString(uuid);
		assertEquals(exp, act);
	}

	TmfExperiment experiment;
	int numTracesPerHost = 2;
	int numHosts = 2;

	@Before
	public void setup() throws IOException, CTFReaderException, TmfTraceException {
		// Create a TMF experiment with 4 traces, 2 traces per host
		File traceDir = TestTraceset.getKernelTrace("sleep-1x-1sec-k");
		ArrayList<ITmfTrace> traces = new ArrayList<ITmfTrace>();
		for (int host = 0; host < numHosts; host++) {
			UUID uuid = UUID.randomUUID();
			for (int i = 0; i < numTracesPerHost; i++) {
				CtfTmfTrace ctfTrace = new CtfTmfTrace();
				ctfTrace.initTrace(null, traceDir.getCanonicalPath(), CtfTmfEvent.class);
				CTFUtils.setTraceClockUUID(ctfTrace.getCTFTrace(), uuid);
				traces.add(ctfTrace);
			}
		}
		ITmfTrace[] a = new ITmfTrace[traces.size()];
		experiment = new TmfExperiment(CtfTmfEvent.class, "foo", traces.toArray(a));
	}

	public CtfTmfEvent[] getOneEventPerTrace(TmfExperiment experiment) {
		ITmfTrace[] traces = experiment.getTraces();
		CtfTmfEvent[] events = new CtfTmfEvent[traces.length];
		for (int i = 0; i < traces.length; i++) {
			ITmfTrace trace = traces[i];
			CtfTmfContext ctx = (CtfTmfContext) trace.seekEvent(0L);
			events[i] = (CtfTmfEvent) trace.getNext(ctx);
		}
		return events;
	}

	public void testModelArrity(IModelKeys key, int exp) {
		CtfTmfEvent[] events = getOneEventPerTrace(experiment);
		HashSet<Object> set = new HashSet<Object>();
		ModelRegistry reg = new ModelRegistry();
		reg.register(key, EventCounter.class);
		for (CtfTmfEvent ev: events) {
			set.add(reg.getModelForTrace(ev.getTrace(), EventCounter.class));
		}
		for (Object obj: set) {
			assertNotNull(obj);
		}
		assertEquals(exp, set.size());
	}

	@Test
	public void testSharedModel() {
		testModelArrity(IModelKeys.SHARED, 1);
	}

	@Test
	public void testPerTraceModel() {
		testModelArrity(IModelKeys.PER_TRACE, numHosts * numTracesPerHost);
	}

	@Test
	public void testPerHostModel() {
		testModelArrity(IModelKeys.PER_HOST, numHosts);
	}

	@Test
	public void testTraceHostMap() {
		TraceReader reader = new TraceReader();
		reader.setTrace(experiment);
		Multimap<Host, CtfTmfTrace> traceHostMap = reader.getTraceHostMap();
		assertEquals(numHosts, traceHostMap.keySet().size());
		assertEquals(numTracesPerHost * numHosts, traceHostMap.keys().size());
	}

}
