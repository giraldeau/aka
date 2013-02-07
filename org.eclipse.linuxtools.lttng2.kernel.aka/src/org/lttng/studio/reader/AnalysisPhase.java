package org.lttng.studio.reader;

import java.util.Collection;

import org.lttng.studio.reader.handler.ITraceEventHandler;


public class AnalysisPhase implements Comparable<AnalysisPhase> {

	private final int rank;
	private final String name;
	private final Collection<ITraceEventHandler> handlers;

	public AnalysisPhase(int rank, String name, Collection<ITraceEventHandler> handlers) {
		this.rank = rank;
		this.name = name;
		this.handlers = handlers;
	}

	public String getName() {
		return this.name;
	}

	public Collection<ITraceEventHandler> getHandlers() {
		return this.handlers;
	}

	@Override
	public int compareTo(AnalysisPhase other) {
		return this.rank > other.rank ? 1 : (this.rank == other.rank ? 0 : -1);
	}

}
