package org.lttng.studio.tests.basic;

import java.util.Collection;

import org.junit.Test;
import org.lttng.studio.reader.handler.ITraceEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;

public class TestTraceReader2 {

	@Test
	public void testProcessMultiPhase() {
		Collection<ITraceEventHandler> phase1 = TraceEventHandlerFactory.makeStatedump();
		Collection<ITraceEventHandler> phase2 = TraceEventHandlerFactory.makeBasic();

	}

}
