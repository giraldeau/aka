package org.lttng.studio.fsa;

import java.util.ArrayList;
/*
 * Records the execution of Automaton
 */

public class AutomatonRecorder<T> {

	ArrayList<T> history;
	Automaton aut;

	public AutomatonRecorder(Automaton aut) {
		this.history = new ArrayList<T>();
		this.aut = aut;
	}

	public Transition step(String event, T e) {
		Transition transition = aut.step(event);
		if (transition != null) {
			history.add(e);
		}
		if (aut.getState() == aut.getInitialState()) {
			history.clear();
		}
		return transition;
	}

	public void reset() {
		aut.reset();
		history.clear();
	}

	public Automaton getAutomaton() {
		return aut;
	}

	public ArrayList<T> getHistory() {
		return history;
	}

}
