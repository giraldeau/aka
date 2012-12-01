package org.lttng.studio.tests.basic;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.linuxtools.ctf.core.event.EventDeclaration;
import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
import org.eclipse.linuxtools.ctf.core.trace.CTFTrace;
import org.eclipse.linuxtools.ctf.core.trace.CTFTraceReader;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class TestSockInetTrace {

	@Test
	public void testInetSockTracepoint() throws IOException, CTFReaderException {
		String[] expEvents = new String[] {"lttng_statedump_inet_sock",
				"inet_sock_local_in", "inet_sock_local_out",
				"inet_sock_create", "inet_sock_clone", "inet_sock_delete",
				"inet_connect", "inet_accept" };
		File trace = TestTraceset.getKernelTrace("netcat-tcp-k");
		CTFTraceReader ctf = new CTFTraceReader(new CTFTrace(trace));
		HashMap<Long,EventDeclaration> events = ctf.getTrace().getEvents(0L);
		HashSet<String> actEvents = new HashSet<String>();
		for (Long id: events.keySet()) {
			actEvents.add(events.get(id).getName());
		}
		HashSet<String> set = new HashSet<String>(Arrays.asList(expEvents));
		SetView<String> res = Sets.intersection(set, actEvents);
		assertEquals(expEvents.length, res.size());
	}

}
