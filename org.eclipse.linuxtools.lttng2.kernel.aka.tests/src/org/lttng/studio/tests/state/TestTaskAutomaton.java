package org.lttng.studio.tests.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.lttng.studio.fsa.Automaton;
import org.lttng.studio.fsa.pattern.BlockingAutomaton;

public class TestTaskAutomaton {

	@Test
	public void testAutomaton() {
		Automaton aut = BlockingAutomaton.getInstance();
		aut.step("sys_entry");
		aut.step("sched_out");
		aut.step("wakeup");
		aut.step("sched_in");
		aut.step("sys_exit");
		assertTrue(aut.getState().isAccepts());
		assertEquals(7, aut.getTransitionSet().size());
	}

}
