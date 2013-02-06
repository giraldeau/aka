package org.lttng.studio.tests.basic;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.request.TmfDataRequest;
import org.junit.Test;

/*
 * Test TmfProvider requests
 */
public class TestTmfTraceReader {

	int count;
	TmfDataRequest request;

	@Test
	public void testReadFromTmfProvider() throws TmfTraceException, IOException, InterruptedException {
		count = 0;
		File trace = TestTraceset.getKernelTrace("sleep-1x-1sec-k");
		CtfTmfTrace ctfTrace = new CtfTmfTrace();
		ctfTrace.initTrace(null, trace.getCanonicalPath(), CtfTmfEvent.class);
		request = new TmfDataRequest(ITmfEvent.class) {
			@Override
			public void handleData(final ITmfEvent event) {
				if (event instanceof CtfTmfEvent) {
					count++;
					request.cancel();
				}
			}
			@Override
			public void handleCancel() {

			}
			@Override
			public void handleSuccess() {

			}
			@Override
			public void handleFailure() {

			}
		};
		ctfTrace.sendRequest(request);
		request.waitForCompletion();
		assertEquals(1, count);
	}

	@Test
	public void testTraceReaderTmf() throws IOException, TmfTraceException {
		File trace = TestTraceset.getKernelTrace("sleep-1x-1sec-k");
		CtfTmfTrace ctfTrace = new CtfTmfTrace();
		ctfTrace.initTrace(null, trace.getCanonicalPath(), CtfTmfEvent.class);
		ctfTrace.seekEvent(Integer.MAX_VALUE);
		System.out.println(ctfTrace.getTimeRange());
	}
}
