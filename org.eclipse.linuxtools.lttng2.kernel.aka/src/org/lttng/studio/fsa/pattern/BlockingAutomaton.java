package org.lttng.studio.fsa.pattern;

import org.lttng.studio.fsa.Automaton;
import org.lttng.studio.fsa.State;
import org.lttng.studio.fsa.Transition;

public class BlockingAutomaton {

	static State s0;
	private BlockingAutomaton() { }

	public static Automaton getInstance() {
		if (s0 == null) {
			s0 = new State(0);
			State s1 = new State(1);
			State s2 = new State(2);
			State s3 = new State(3);
			State s4 = new State(4);
			State s5 = new State(5);
			s5.setAccepts(true);

			s0.addTransition(new Transition("sys_entry", s1));
			s1.addTransition(new Transition("sys_exit", s0));
			s1.addTransition(new Transition("sched_out", s2));
			s2.addTransition(new Transition("sched_in", s1));
			s2.addTransition(new Transition("wakeup", s3));
			s3.addTransition(new Transition("sched_in", s4));
			s4.addTransition(new Transition("sys_exit", s5));
		}
		return new Automaton(s0);
	}

}
