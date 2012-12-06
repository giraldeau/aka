package org.lttng.studio.model.graph;

public class ExecVertex {

	private static int count = 0;

	private final long timestamp;
	private final Object owner;
	private final int id;
	private VertexType type;

	public ExecVertex(Object owner, long timestamp) {
		this(owner, timestamp, VertexType.DEFAULT);
	}

	public ExecVertex(Object owner, long timestamp, VertexType type) {
		this.owner = owner;
		this.timestamp = timestamp;
		this.id = count++;
		this.type = type;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Object getOwner() {
		return owner;
	}

	@Override
	public String toString() {
		return String.format("[%d] %s %d", id, this.owner.toString(), timestamp);
	}

	public int getId() {
		return id;
	}

	public VertexType getType() {
		return type;
	}

	public void setType(VertexType type) {
		this.type = type;
	}

}
