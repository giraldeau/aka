package org.lttng.studio.fsa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class Automaton {
	private State state;
	private final State initial;
	public Automaton(State initial) {
		this.initial = initial;
		this.setState(initial);
	}
	public State getState() {
		return state;
	}
	public void setState(State state) {
		this.state = state;
	}
	public Set<Transition> getTransitionSet() {
		HashSet<Transition> t = new HashSet<Transition>();
		HashSet<State> set = getStateSet();
		for (State s: set) {
			t.addAll(s.getTransitions());
		}
		return t;
	}
	public Set<String> getSymbols() {
		Set<Transition> trans = getTransitionSet();
		HashSet<String> syms = new HashSet<String>();
		for (Transition t: trans) {
			syms.add(t.getEvent());
		}
		return syms;
	}

	public Transition step(String event) {
		State curr = getState();
		ArrayList<Transition> transitions = curr.getTransitions();
		for (Transition t: transitions) {
			if (t.getEvent().equals(event)) {
				setState(t.getTo());
				return t;
			}
		}
		return null;
	}

	public void reset() {
		state = initial;
	}

	public HashSet<State> getStateSet() {
		HashSet<State> visited = new HashSet<State>();
		Stack<State> stack = new Stack<State>();
		stack.push(initial);
		while (!stack.isEmpty()) {
			State curr = stack.pop();
			if (visited.contains(curr))
				continue;
			visited.add(curr);
			ArrayList<Transition> trans = curr.getTransitions();
			for (Transition tran: trans) {
				stack.push(tran.getTo());
			}
		}
		return visited;
	}
	public State getInitialState() {
		return initial;
	}
}