package org.lttng.studio.model.graph;

public class ExecVertex {

	private final long timestamp;
	private final Object owner;
	private final boolean inSoftirq;

	public ExecVertex(Object owner, long timestamp, boolean inSoftirq) {
		this.owner = owner;
		this.timestamp = timestamp;
		this.inSoftirq = inSoftirq;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Object getOwner() {
		return owner;
	}

	public boolean isInSoftirq() {
		return inSoftirq;
	}

	@Override
	public String toString() {
		return this.owner.toString();
	}

}
