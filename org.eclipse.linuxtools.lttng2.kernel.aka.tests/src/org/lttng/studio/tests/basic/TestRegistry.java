package org.lttng.studio.tests.basic;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
import org.eclipse.linuxtools.ctf.core.trace.CTFTrace;
import org.eclipse.linuxtools.ctf.core.trace.CTFTraceReader;
import org.junit.Test;

public class TestRegistry {

	@Test
	public void testRecoverHost() throws IOException, CTFReaderException {
		String uuid = "df194be5-29ec-4143-9d8c-12be07862030";
		File traceDir = TestTraceset.getKernelTrace("netcat-udp-k");
		CTFTrace trace = new CTFTrace(traceDir);
		CTFTraceReader reader = new CTFTraceReader(trace);
		reader.getTrace().getClock().addAttribute("uuid", "\"" + uuid + "\"");
		String property = (String) reader.getTrace().getClock().getProperty("uuid");
		// unquote if necessary
		if (property.matches("\"[a-z0-9-]*\""))
			property = property.substring(1, property.length() - 1);
		UUID act = UUID.fromString(property);
		UUID exp = UUID.fromString(uuid);
		assertEquals(exp, act);
	}

}
