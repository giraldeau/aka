package org.lttng.studio.tests.graph;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.lttng.studio.model.graph.Span;

public class TestSpan {

	static Object A = "A";
	static Object B = "B";
	static Object C = "C";
	static Object D = "D";

	public static Span makeSpanTree1() {
		Span root = new Span("root");
		Span spanA = new Span(A);
		Span spanB = new Span(B);
		Span spanC = new Span(C);
		Span spanD = new Span(D);
		spanA.setParentAndChild(root);
		spanB.setParentAndChild(spanA);
		spanC.setParentAndChild(spanB);
		spanD.setParentAndChild(spanB);

		spanA.addSelfTime(1);
		spanB.addSelfTime(2);
		spanC.addSelfTime(3);
		spanD.addSelfTime(4);

		root.computeTotalTime();
		return root;
	}


	@Test
	public void testSpanStats() {
		Span root = makeSpanTree1();
		assertEquals(10, root.getTotalTime());
		assertEquals(0, root.getSelfTime());
		assertEquals(1, root.getChildrenTime());
	}

}
