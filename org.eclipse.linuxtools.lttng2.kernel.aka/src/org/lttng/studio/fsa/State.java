package org.lttng.studio.fsa;

import java.util.ArrayList;

public class State {
	private int id;
	ArrayList<Transition> t;
	private boolean accepts;
	public State(int id) {
		this.setId(id);
		this.t = new ArrayList<Transition>();
	}
	public void addTransition(Transition tran) {
		t.add(tran);
	}
	public ArrayList<Transition> getTransitions() {
		return t;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public boolean isAccepts() {
		return accepts;
	}
	public void setAccepts(boolean accepts) {
		this.accepts = accepts;
	}
	@Override
	public String toString() {
		return Integer.toString(id);
	}
}