package org.lttng.studio.reader;

import java.util.Collection;

import org.lttng.studio.reader.handler.ITraceEventHandler;


public class AnalysisPhase {

	private final String name;
	private final Collection<ITraceEventHandler> handlers;

	public AnalysisPhase(String name, Collection<ITraceEventHandler> handlers) {
		this.name = name;
		this.handlers = handlers;
	}

	public String getName() {
		return this.name;
	}

	public Collection<ITraceEventHandler> getHandlers() {
		return this.handlers;
	}

}
