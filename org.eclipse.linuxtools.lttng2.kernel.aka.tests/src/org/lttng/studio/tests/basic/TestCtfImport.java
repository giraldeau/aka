package org.lttng.studio.tests.basic;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
import org.eclipse.linuxtools.ctf.core.trace.CTFTrace;
import org.eclipse.linuxtools.ctf.core.trace.CTFTraceReader;
import org.junit.Test;

public class TestCtfImport {

	@Test
	public void testDummyException() throws CTFReaderException, IOException {
		CTFReaderException ex = new CTFReaderException();
		assertTrue(ex.toString().contains("CTFReaderException"));
	}
	
	@Test
	public void testCtfImport() throws CTFReaderException, IOException {
		File traceDir = TestTraceset.getKernelTrace("netcat-udp-k");
		CTFTrace trace = new CTFTrace(traceDir);
		CTFTraceReader reader = new CTFTraceReader(trace);
		assertTrue(reader.getStartTime() > 0);
	}
	
	@Test
	public void testTraceNotFound() throws CTFReaderException {
		Exception exception = null;
		try {
			TestTraceset.getKernelTrace("not-a-trace");
		} catch (IOException e) {
			exception = e;
		}
		assertNotNull(exception);
	}
	
}
