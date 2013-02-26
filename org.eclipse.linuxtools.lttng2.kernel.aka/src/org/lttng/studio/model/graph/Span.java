package org.lttng.studio.model.graph;

import java.util.ArrayList;
import java.util.List;

public class Span implements Comparable<Span> {

	private final Object owner;
	private int count;
	private long total;
	private long min = Integer.MAX_VALUE;
	private long max = Integer.MIN_VALUE;
	private ArrayList<Span> children;
	private Span parent;

	public Span(Object owner) {
		this.owner = owner;
	}

	public Object getOwner() {
		return owner;
	}

	public Span getParent() {
		return parent;
	}

	public void setParent(Span parent) {
		this.parent = parent;
	}

	public void setParentAndChild(Span parent) {
		setParent(parent);
		if (parent != null)
			parent.addChild(this);
	}

	public List<Span> getChildren() {
		if (children == null)
			return new ArrayList<Span>();
		return children;
	}

	public void addChild(Span child) {
		if (children == null)
			children = new ArrayList<Span>();
		children.add(child);
	}

	public void removeChild(Span child) {
		if (children != null) {
			children.remove(child);
			if (children.isEmpty()) {
				children = null;
			}
		}
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
		return this.total > other.total ? 1 : (this.total == other.total ? 0 : -1);
	}

	@Override
	public String toString() {
		return "" + this.total;
	}

}
