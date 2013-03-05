package org.lttng.studio.model.graph;

import java.util.ArrayList;
import java.util.List;

public class Span implements Comparable<Span> {

	private final Object owner;
	private int count;
	private long selfTime;
	private long totalTime;
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
		if (this.parent != null)
			this.parent.removeChild(this);
		if (parent != null)
			parent.addChild(this);
		setParent(parent);
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

	public long getSelfTime() {
		return selfTime;
	}

	public void addSelfTime(long time) {
		this.count++;
		this.min = Math.min(time, min);
		this.max = Math.min(time, max);
		this.selfTime += time;
	}

	public long getMin() {
		return min;
	}

	public long getMax() {
		return max;
	}

	@Override
	public int compareTo(Span other) {
		return this.selfTime > other.selfTime ? 1 : (this.selfTime == other.selfTime ? 0 : -1);
	}

	@Override
	public String toString() {
		return "[" + this.owner + "," + this.parent + "]";
	}

	public void computeTotalTime() {
		totalTime = selfTime;
		if (children == null || children.isEmpty())
			return;
		for (Span child: children) {
			child.computeTotalTime();
			totalTime += child.getTotalTime();
		}
	}

	public long getTotalTime() {
		return totalTime;
	}

	public long getChildrenTime() {
		long time = 0;
		if (children == null || children.isEmpty())
			return time;
		for (Span child: children) {
			time += child.getSelfTime();
		}
		return time;
	}

}
