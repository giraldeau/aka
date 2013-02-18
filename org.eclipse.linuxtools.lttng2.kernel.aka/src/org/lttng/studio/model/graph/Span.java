package org.lttng.studio.model.graph;

public class Span implements Comparable<Span> {

	private final Object owner;
	private int count;
	private long total;
	private long min = Integer.MAX_VALUE;
	private long max = Integer.MIN_VALUE;

	public Span(Object owner) {
		this.owner = owner;
	}

	public Object getOwner() {
		return owner;
	}

	public int getCount() {
		return count;
	}

	public long getTotal() {
		return total;
	}

	public void addSelf(long time) {
		this.count++;
		this.min = Math.min(time, min);
		this.max = Math.min(time, max);
		this.total += time;
	}

	public long getMin() {
		return min;
	}

	public long getMax() {
		return max;
	}

	@Override
	public int compareTo(Span other) {
		return this.total > other.total ? 1 : (this.total == other.total ? 0 : 1);
	}

	@Override
	public String toString() {
		return "" + this.total;
	}

}
