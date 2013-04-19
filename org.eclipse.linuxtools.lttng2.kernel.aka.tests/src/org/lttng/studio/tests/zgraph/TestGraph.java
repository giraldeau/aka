package org.lttng.studio.tests.zgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.lttng.studio.model.zgraph.Dot;
import org.lttng.studio.model.zgraph.Graph;
import org.lttng.studio.model.zgraph.LinkType;
import org.lttng.studio.model.zgraph.Node;
import org.lttng.studio.model.zgraph.Operations;
import org.lttng.studio.model.zgraph.Ops;

public class TestGraph {

	private static Object A = "A";
	private static Object B = "B";

	public static void writeString(Object writer, String fname, String content) {
		String folder = writer.getClass().getName();
		try {
			File dir = new File("results", folder);
			dir.mkdirs();
			File fout = new File(dir, fname);
			FileWriter fwriter = new FileWriter(fout);
			fwriter.write(content);
			fwriter.flush();
			fwriter.close();
			System.out.println("wrote " + fout);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCreateGraph() {
		Graph g = new Graph();
		assertEquals(0, g.getNodesOf(A).size());
	}

	@Test
	public void testPutNode() {
		Graph g = new Graph();
		g.replace(A, new Node(0));
		g.replace(A, new Node(1));
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
			assertEquals(n0.right(), n1);
			assertEquals(n1.left(), n0);
			assertEquals(n0.links[Node.RIGHT].from, n0);
			assertEquals(n1.links[Node.LEFT].to, n1);
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

	@Test
	public void testDot() {
		int max = 10;
		Graph g = new Graph();
		for (int i = 0; i < max; i++) {
			g.append(A, new Node(i));
			g.append(B, new Node(i));
		}
		List<Node> la = g.getNodesOf(A);
		List<Node> lb = g.getNodesOf(B);
		la.get(0).linkVertical(la.get(2));
		la.get(1).linkVertical(lb.get(1));
		lb.get(5).linkVertical(la.get(6));
		writeString(this, "full.dot", Dot.todot(g));
		List<Object> list = new LinkedList<Object>();
		list.add(A);
		writeString(this, "partial.dot", Dot.todot(g, list));
	}

	@Test
	public void testCheckHorizontal() {
		Node n0 = new Node(10);
		Node n1 = new Node(0);
		Exception exception = null;
		try {
			n0.linkHorizontal(n1);
		} catch (IllegalArgumentException e) {
			exception = e;
		}
		assertNotNull(exception);
	}

	@Test
	public void testCheckVertical() {
		Node n0 = new Node(10);
		Node n1 = new Node(0);
		Exception exception = null;
		try {
			n0.linkVertical(n1);
		} catch (IllegalArgumentException e) {
			exception = e;
		}
		assertNotNull(exception);
	}

	@Test
	public void testValidate() {
		Node n0 = new Node(10);
		Node n1 = new Node(0);
		n0.linkHorizontalRaw(n1);
		assertFalse(Ops.validate(n0));
	}

	@Test
	public void testMakeGraphBasic() {
		Node head = Ops.basic(10);
		writeString(this, "basic.dot", Dot.todot(head));
		assertTrue(Ops.validate(head));
	}

	@Test
	public void testSize() {
		Node node = new Node(0);
		Node head = node;
		for (int i = 0; i < 10; i++) {
			Node next = new Node(i);
			node.linkHorizontalRaw(next);
			node = next;
		}
		Graph g = Ops.toGraph(head);
		assertEquals(Ops.size(head), g.size());
	}

	@Test
	public void testOffset() {
		Node head = Ops.basic(10);
		Ops.offset(head, 100);
		writeString(this, "offset.dot", Dot.todot(head));
		assertTrue(Ops.validate(head));
	}

	@Test
	public void testClone1() {
		Node head = Ops.basic(10);
		Node clone = Ops.clone(head);
		assertEquals(Ops.size(head), Ops.size(clone));
		assertTrue(Ops.validate(clone));
	}

	@Test
	public void testConcat() {
		Node n1 = Ops.basic(1);
		Node n2 = Ops.basic(1);
		Node head = Ops.concat(n1, n2);
		writeString(this, "concat.dot", Dot.todot(head));
		assertEquals(Ops.size(head), Ops.size(n1) + Ops.size(n2) + 2);
		assertTrue(Ops.validate(head));
	}

	@Test
	public void testIter() {
		Node n = Ops.basic(10);
		Node head = Ops.iter(n, 1);
		writeString(this, "iter.dot", Dot.todot(head));
		assertEquals(2 + 3 + 3 + 2, Ops.size(head));
		assertTrue(Ops.validate(head));
	}

	@Test
	public void testUnionLeft() {
		Node n1 = Ops.basic(20);
		Node n2 = Ops.basic(10);
		Ops.unionInPlaceLeft(n1, n2, LinkType.DEFAULT);
		writeString(this, "union_left.dot", Dot.todot(n1));
		assertEquals(16, Ops.size(n1));
		assertTrue(Ops.validate(n1));
	}

	@Test
	public void testUnionRight() {
		Node n1 = Ops.basic(20);
		Node n2 = Ops.basic(10);
		Ops.unionInPlaceRight(n1, n2, LinkType.DEFAULT);
		writeString(this, "union_right.dot", Dot.todot(n1));
		assertEquals(16, Ops.size(n1));
		assertTrue(Ops.validate(n1));
	}

	@Test
	public void testUnion1() {
		Node n1 = Ops.basic(10);
		Node n2 = Ops.basic(10);
		Node u1 = Ops.union(n1, n2);
		writeString(this, "union.dot", Dot.todot(u1));
		assertEquals(3 * 8, Ops.size(u1));
		assertTrue(Ops.validate(u1));
	}

	@Test
	public void testInsertAfter() {
		Node head = Ops.basic(2);
		Node node = new Node(1);
		Node exp = Ops.sequence(3, 1);
		Ops.insertAfter(head, node);
		assertEquals(Ops.size(exp), Ops.size(head));
		assertTrue(Ops.match(head, exp));
	}

	@Test
	public void testInsertBerfore() {
		Node head = Ops.basic(2);
		Node node = new Node(1);
		Node exp = Ops.sequence(3, 1);
		Ops.insertBefore(Ops.tail(head), node);
		assertEquals(Ops.size(exp), Ops.size(head));
		assertTrue(Ops.match(head, exp));
	}

	@Test
	public void testInsertAfterMulti() {
		Node head = Ops.basic(9);
		Node node = Ops.sequence(2, 3);
		Ops.offset(node, 3);
		Node exp = Ops.sequence(4, 3);
		Ops.insertAfter(head, node);
		assertEquals(Ops.size(exp), Ops.size(head));
		assertTrue(Ops.match(head, exp));
	}

	@Test
	public void testInsertBerforeMulti() {
		Node head = Ops.basic(9);
		Node node = Ops.sequence(2, 3);
		Ops.offset(node, 3);
		Node exp = Ops.sequence(4, 3);
		Ops.insertBefore(Ops.tail(head), node);
		assertEquals(Ops.size(exp), Ops.size(head));
		assertTrue(Ops.match(head, exp));
	}

	@Test
	public void testMerge() {
		Node n1 = Ops.sequence(3, 2);
		Node n2 = Ops.sequence(3, 2);
		Node exp = Ops.sequence(6, 1);
		Ops.offset(n2, 1);
		Node res = Ops.merge(n1, n2);
		writeString(this, "merge_1.dot", Dot.todot(n1));
		writeString(this, "merge_2.dot", Dot.todot(n2));
		writeString(this, "merge.dot", Dot.todot(res));
		assertEquals(Ops.size(exp), Ops.size(res));
		assertTrue(Ops.match(res, exp));
	}

	@Test
	public void testAlignRight() {
		Node n1 = Ops.sequence(3, 4);
		Ops.offset(n1, 100);

		Node exp = Ops.sequence(3, 2);
		Ops.offset(exp, 100 + 4);

		for (int i = -9; i < 10; i++) {
			Node n2 = Ops.sequence(3, 2);
			Ops.offset(n2, i);
			Ops.alignRight(n1, n2);
			assertTrue(Ops.match(n2, exp));
		}
	}

	@Test
	public void testAlignLeft() {
		Node n1 = Ops.sequence(3, 4);
		Ops.offset(n1, 100);

		Node exp = Ops.sequence(3, 2);
		Ops.offset(exp, 100);

		for (int i = -9; i < 10; i++) {
			Node n2 = Ops.sequence(3, 2);
			Ops.offset(n2, i);
			Ops.alignLeft(n1, n2);
			assertTrue(Ops.match(n2, exp));
		}
	}

	@Test
	public void testAlignCenter1() {
		Node n1 = Ops.sequence(3, 4);
		Ops.offset(n1, 100);

		Node exp = Ops.sequence(3, 2);
		Ops.offset(exp, 100 + 2);

		for (int i = -9; i < 10; i++) {
			Node n2 = Ops.sequence(3, 2);
			Ops.offset(n2, i);
			Ops.alignCenter(n1, n2);
			assertTrue(Ops.match(n2, exp));
		}
	}

	@Test
	public void testAlignCenter2() {
		Node n1 = Ops.sequence(3, 2);
		Ops.offset(n1, 100);

		Node exp = Ops.sequence(3, 4);
		Ops.offset(exp, 100 - 2);

		for (int i = -9; i < 10; i++) {
			Node n2 = Ops.sequence(3, 4);
			Ops.offset(n2, i);
			Ops.alignCenter(n1, n2);
			assertTrue(Ops.match(n2, exp));
		}
	}

	@Test
	public void testCheckMatchOk() {
		Node n1 = Ops.basic(10);
		Node n2 = Ops.clone(n1);
		assertTrue(Ops.match(n1, n2));
		assertTrue(Ops.match(n1, n2, Ops.MATCH_ISOMORPH));
		assertTrue(Ops.match(n1, n2, Ops.MATCH_TIMESTAMPS));
		assertTrue(Ops.match(n1, n2, Ops.MATCH_LINKS_TYPE));
	}

	@Test
	public void testCheckEqualityMatchFailTimestamps() {
		Node n1 = Ops.basic(10);
		Node n2 = Ops.basic(11);
		assertFalse(Ops.match(n1, n2));
		assertTrue(Ops.match(n1, n2, Ops.MATCH_ISOMORPH));
		assertFalse(Ops.match(n1, n2, Ops.MATCH_TIMESTAMPS));
		assertTrue(Ops.match(n1, n2, Ops.MATCH_LINKS_TYPE));
	}

	@Test
	public void testCheckEqualityMatchFailNode() {
		Node n1 = Ops.iter(Ops.basic(10), 2);
		Node n2 = Ops.basic(10);
		assertFalse(Ops.match(n1, n2));
		assertFalse(Ops.match(n1, n2, Ops.MATCH_ISOMORPH));
		assertFalse(Ops.match(n1, n2, Ops.MATCH_TIMESTAMPS));
		assertFalse(Ops.match(n1, n2, Ops.MATCH_LINKS_TYPE));
	}

	@Test
	public void testCheckEqualityMatchFailLink() {
		Node n1 = Ops.basic(10);
		n1.links[Node.RIGHT].type = LinkType.EPS;
		Node n2 = Ops.basic(10);
		n2.links[Node.RIGHT].type = LinkType.DEFAULT;
		assertFalse(Ops.match(n1, n2));
		assertTrue(Ops.match(n1, n2, Ops.MATCH_ISOMORPH));
		assertTrue(Ops.match(n1, n2, Ops.MATCH_TIMESTAMPS));
		assertFalse(Ops.match(n1, n2, Ops.MATCH_LINKS_TYPE));
	}

	@Test
	public void testCriticalPath1() {
		Graph g = new Graph();
		for (int i = 0; i < 10; i++) {
			g.append(A, new Node(i * 10));
		}
		Graph path = Operations.criticalPath(g, A, 25, 75);
		String content = Dot.todot(g);
		writeString(this, "task1_full.dot", content);
		content = Dot.todot(path);
		writeString(this, "task1_A.dot", content);
		assertEquals(7, path.getNodesOf(A).size());
	}

}
