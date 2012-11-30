package org.lttng.studio.fsa;

public class Transition {
	private String event;
	private State to;
	public Transition(String ev, State to) {
		this.event = ev;
		this.to = to;
	}
	public String getEvent() {
		return event;
	}
	public void setEvent(String event) {
		this.event = event;
	}
	public State getTo() {
		return to;
	}
	public void setTo(State to) {
		this.to = to;
	}
}