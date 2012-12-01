package org.lttng.studio.tests.sock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.linuxtools.ctf.core.trace.CTFTrace;
import org.eclipse.linuxtools.ctf.core.trace.CTFTraceReader;
import org.junit.Test;
import org.lttng.studio.model.kernel.Inet4Sock;
import org.lttng.studio.model.kernel.ModelRegistry;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.reader.handler.StatedumpInetSockEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerSched;
import org.lttng.studio.reader.handler.TraceEventHandlerSock;
import org.lttng.studio.tests.basic.TestTraceset;

import com.google.common.collect.BiMap;

public class TestInetSock {

	@Test
	public void testInetSockStatedump() throws Exception {
		File trace = TestTraceset.getKernelTrace("netcat-tcp-k");
		TraceReader reader = new TraceReader();
		reader.addReader(new CTFTraceReader(new CTFTrace(trace)));
		reader.register(new StatedumpInetSockEventHandler());
		reader.process();
		SystemModel model = ModelRegistry.getInstance().getModel(reader, SystemModel.class);
		assertTrue(model.getInetSocks().size() > 0);
	}

	@Test
	public void testInetSockSteadyState() throws Exception {
		File trace = TestTraceset.getKernelTrace("netcat-tcp-k");
		TraceReader reader = new TraceReader();
		reader.addReader(new CTFTraceReader(new CTFTrace(trace)));
		reader.register(new TraceEventHandlerSched());
		reader.register(new TraceEventHandlerSock());
		reader.process();
		SystemModel model = ModelRegistry.getInstance().getModel(reader, SystemModel.class);
		BiMap<Inet4Sock, Inet4Sock> socks = model.getInetSockIndex();
		//System.out.println(socks);
		assertEquals(1, socks.size());

		Inet4Sock sock1 = socks.keySet().iterator().next();
		Inet4Sock sock2 = socks.get(sock1);
		Task owner1 = model.getInetSockTaskOwner(sock1);
		Task owner2 = model.getInetSockTaskOwner(sock2);
		assertTrue(owner1.getName().endsWith("netcat"));
		assertTrue(owner2.getName().endsWith("netcat"));
	}

}
