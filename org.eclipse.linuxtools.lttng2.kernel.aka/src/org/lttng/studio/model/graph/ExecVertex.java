package org.lttng.studio.model.graph;

public class ExecVertex {

	private static int count = 0;

	private final long timestamp;
	private final Object owner;
	private final boolean inSoftirq;
	private final int id;

	public ExecVertex(Object owner, long timestamp, boolean inSoftirq) {
		this.owner = owner;
		this.timestamp = timestamp;
		this.inSoftirq = inSoftirq;
		this.id = count++;
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

	public int getId() {
		return id;
	}

}
