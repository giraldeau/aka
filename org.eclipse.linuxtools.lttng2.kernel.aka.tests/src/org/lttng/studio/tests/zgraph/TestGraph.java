package org.lttng.studio.tests.zgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.lttng.studio.model.zgraph.Graph;
import org.lttng.studio.model.zgraph.Node;

public class TestGraph {

	private static Object A = "A";

	@Test
	public void testCreateGraph() {
		Graph g = new Graph();
		assertEquals(0, g.getNodesOf(A).size());
	}

	@Test
	public void testPutNode() {
		Graph g = new Graph();
		g.put(A, new Node(0));
		g.put(A, new Node(1));
		assertEquals(1, g.getNodesOf(A).size());
	}

	@Test
	public void testAppendNode() {
		Graph g = new Graph();
		int max = 10;
		for(int i = 0; i < max; i++)
			g.append(A, new Node(i));
		List<Node> list = g.getNodesOf(A);
		assertEquals(max, list.size());
		checkLinkHorizontal(list);
	}

	private void checkLinkHorizontal(List<Node> list) {
		if (list.isEmpty())
			return;
		for (int i = 0; i < list.size() - 1; i++) {
			Node n0 = list.get(i);
			Node n1 = list.get(i+1);
			assertEquals(n0.next.from, n0);
			assertEquals(n1.prev.to, n1);
			assertEquals(n0.next.to, n1);
			assertEquals(n1.prev.from, n0);
		}
	}

	@Test
	public void testIllegalNode() {
		Graph g = new Graph();
		g.append(A, new Node(1));
		Exception exception = null;
		try {
			g.append(A, new Node(0));
		} catch (IllegalArgumentException e) {
			exception = e;
		}
		assertNotNull(exception);
	}

}
