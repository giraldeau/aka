package org.lttng.studio.model.zgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Stack;

public class Ops {

	private interface Visitor {
		public void visitHead(Node node);
		public void visitNode(Node node);
		public void visitLink(Link link, boolean horizontal);
	}

	private static class ScanLineTraverse {
		public static void traverse(Node start, Visitor visitor) {
			Stack<Node> stack = new Stack<Node>();
			HashSet<Node> visited = new HashSet<Node>();
			stack.add(start);
			while(!stack.isEmpty()) {
				Node curr = stack.pop();
				if (visited.contains(curr))
					continue;
				//  process one line
				Node n = head(curr);
				visitor.visitHead(n);
				while(n != null) {
					visitor.visitNode(n);
					// Only visit links up-right, guarantee to visit once only
					if (n.up() != null) {
						stack.push(n.up());
						visitor.visitLink(n.links[Node.UP], false);
					}
					if (n.down() != null)
						stack.push(n.down());
					if (n.right() != null)
						visitor.visitLink(n.links[Node.RIGHT], true);
					visited.add(n);
					n = n.right();
				}
			}
		}
	}

	private static Node epsilon(Node node, int direction) {
		Node eps = new Node(node);
		Link link = null;
		switch(direction) {
		case Node.LEFT:
			link = eps.linkHorizontal(node);
			break;
		case Node.RIGHT:
			link = node.linkHorizontal(eps);
			break;
		case Node.UP:
			link = node.linkVertical(eps);
			break;
		case Node.DOWN:
			link = eps.linkVertical(node);
			break;
		default:
			throw new IllegalArgumentException("invalid direction: " + direction);
		}
		link.type = LinkType.EPS;
		return eps;
	}

	public static Node basic(long len) {
		return basic(len, LinkType.DEFAULT);
	}

	public static Node basic(long len, LinkType type) {
		Node head = new Node(0);
		Link link = head.linkHorizontal(new Node(len));
		link.type = type;
		return head;
	}

	private static class CloneState {
		HashMap<Node, Node> map = new HashMap<Node, Node>(); // orig, clone
		Node head;
	}

	/**
	 * Clone the provided connected node set. Returns the head of the new node sequence.
	 * @param orig
	 * @return
	 */
	public static Node clone(Node orig) {
		// two steps clone:
		// 1- clone all nodes
		// 2- create links
		final CloneState state = new CloneState();
		ScanLineTraverse.traverse(orig, new Visitor() {
			@Override
			public void visitHead(Node node) {
			}
			@Override
			public void visitNode(Node node) {
				Node clone = new Node(node);
				state.map.put(node, clone);
			}
			@Override
			public void visitLink(Link link, boolean hori) {
			}
		});
		// FIXME: can iterate over map keys
		ScanLineTraverse.traverse(orig, new Visitor() {
			@Override
			public void visitHead(Node node) {
				if (state.head == null)
					state.head = state.map.get(node);
			}
			@Override
			public void visitNode(Node node) {
			}
			@Override
			public void visitLink(Link link, boolean hori) {
				Node from = state.map.get(link.from);
				Node to = state.map.get(link.to);
				Link lnk = null;
				if (hori) {
					lnk = from.linkHorizontal(to);
				} else {
					lnk = from.linkVertical(to);
				}
				lnk.type = link.type;
			}
		});
		return state.head;
	}

	/**
	 * Concatenate two node sequences. Offset is applied to sequence N2 to
	 * create EPS transition between the tail of N1 and head of N2.
	 * @param n1
	 * @param n2
	 * @return
	 */
	public static Node concat(Node n1, Node n2) {
		Node c1 = Ops.clone(n1);
		Node c2 = Ops.clone(n2);
		concatInPlace(c1, c2);
		return c1;
	}

	public static void concatInPlace(Node n1, Node n2) {
		Node x = Ops.tail(n1);
		Node y = Ops.head(n2);
		long offset = x.getTs() - y.getTs();
		Ops.offset(n2, offset);
		Link link = x.linkHorizontal(y);
		link.type = LinkType.EPS;
	}

	/**
	 * Create parallel paths by connecting heads and tails of N1 and N2.
	 * @param n1
	 * @param n2
	 * @return
	 */
	public static Node union(Node n1, Node n2) {
		return union(n1, n2, LinkType.DEFAULT, LinkType.DEFAULT);
	}

	public static Node union(Node n1, Node n2, LinkType split, LinkType merge) {
		Node h1 = Ops.clone(n1);
		Node h2 = Ops.clone(n2);
		return unionInPlace(h1, h2, split, merge);
	}

	public static Node unionInPlace(Node n1, Node n2, LinkType split, LinkType merge) {
		unionInPlaceRight(n1, n2, merge);
		return unionInPlaceLeft(n1, n2, split);
	}

	public static Node unionInPlaceLeft(Node n1, Node n2, LinkType split) {
		Node h1 = Ops.head(n1);
		Node h2 = Ops.head(n2);
		// add epsilon nodes to prevent overwrite of existing links
		h1 = Ops.epsilon(h1, Node.LEFT);
		h2 = Ops.epsilon(h2, Node.LEFT);
		Link l1 = h1.linkVertical(h2);
		l1.type = split;
		return h1;
	}

	public static Node unionInPlaceRight(Node n1, Node n2, LinkType merge) {
		Node t1 = Ops.tail(n1);
		Node t2 = Ops.tail(n2);
		t1 = Ops.epsilon(t1, Node.RIGHT);
		t2 = Ops.epsilon(t2, Node.RIGHT);
		Link l2 = t2.linkVertical(t1);
		l2.type = merge;
		return t1;
	}

	/**
	 * Create a vertical link within the same node sequence.
	 * @param n1
	 * @param n2
	 * @param type
	 * @return
	 */
	public static void unionShortcut(Node n1, Node n2, LinkType type) {
		Link link = n1.linkVertical(n2);
		link.type = type;
	}

	/**
	 * Repeat the node sequence
	 * @param node
	 * @param repeat
	 * @return
	 */
	public static Node iter(Node node, int repeat) {
		Node clone = Ops.clone(node);
		iterInPlace(clone, repeat);
		return clone;
	}

	public static void iterInPlace(Node node, int repeat) {
		Node curr = node;
		for (int i = 0; i < repeat; i++) {
			Node clone = Ops.clone(node);
			Ops.concatInPlace(curr, clone);
			curr = Ops.tail(clone);
		}
	}

	/**
	 * Insert N2 and its siblings on the left of N1
	 * @param n1
	 * @param n2
	 */
	public static void insertBefore(Node n1, Node n2) {
		if (n1.hasNeighbor(Node.LEFT)) {
			n1.neighbor(Node.LEFT).linkHorizontal(n2);
		}
		Node tail = tail(n2);
		tail.linkHorizontal(n1);
	}

	/**
	 * Insert N2 and its siblings on the right of N1
	 * @param n1
	 * @param n2
	 */
	public static void insertAfter(Node n1, Node n2) {
		if (n1.hasNeighbor(Node.RIGHT)) {
			Node tail = tail(n2);
			tail.linkHorizontal(n1.neighbor(Node.RIGHT));
		}
		n1.linkHorizontal(n2);
	}

	/**
	 * Generate an horizontal sequence of NUM nodes with STEP timestamps
	 * @param num
	 * @param step
	 * @return
	 */
	public static Node sequence(int num, int step) {
		Node curr = null;
		Node next = null;
		for (int i = 0; i < num; i++) {
			next = new Node(i * step);
			if (curr != null) {
				curr.linkHorizontal(next);
			}
			curr = next;
		}
		return head(curr);
	}

	/**
	 * Merge N2 sequence into N1 according to timestamps. The resulting order of
	 * nodes with equal timestamps is undefined.
	 * @param n1
	 * @param n2
	 */
	public static Node merge(Node n1, Node n2) {
		Node c1 = Ops.clone(n1);
		Node c2 = Ops.clone(n2);
		Ops.mergeInPlace(c1, c2);
		return c1;
	}

	public static void mergeInPlace(Node n1, Node n2) {
		PriorityQueue<Node> queue = new PriorityQueue<Node>();
		queue.add(n1);
		queue.add(n2);
		Node prev = null;
		while(!queue.isEmpty()) {
			Node curr = queue.poll();
			if (curr.hasNeighbor(Node.RIGHT))
				queue.add(curr.neighbor(Node.RIGHT));
			if (prev != null)
				prev.linkHorizontal(curr);
			prev = curr;
		}
	}

	/**
	 * Align tail timestamps of N2 according to tail of N1
	 * @param n1
	 * @param n2
	 */
	public static void alignRight(Node n1, Node n2) {
		Node t1 = tail(n1);
		Node t2 = tail(n2);
		long diff = t1.getTs() - t2.getTs();
		offset(n2, diff);
	}

	/**
	 * Align head timestamps of N2 according to head of N1
	 * @param n1
	 * @param n2
	 */
	public static void alignLeft(Node n1, Node n2) {
		Node h1 = head(n1);
		Node h2 = head(n2);
		long diff = h1.getTs() - h2.getTs();
		offset(n2, diff);
	}

	/**
	 * Align N2 timestamps according to N1 center.
	 * @param n1
	 * @param n2
	 */
	public static void alignCenter(Node n1, Node n2) {
		long h1 = head(n1).getTs();
		long h2 = head(n2).getTs();
		long shift = h1 - h2;
		long d1 = (tail(n1).getTs() - head(n1).getTs());
		long d2 = (tail(n2).getTs() - head(n2).getTs());
		long diff = shift + ((d1 - d2) / 2);
		offset(n2, diff);
	}

	public static Node minimize(Node node) {
		throw new UnsupportedOperationException();
	}

	public static Node tail(Node node) {
		while(node.right() != null) {
			node = node.right();
		}
		return node;
	}

	public static Node head(Node node) {
		while(node.left() != null) {
			node = node.left();
		}
		return node;
	}

	public static void offset(Node node, final long offset) {
		if (offset == 0)
			return;
		ScanLineTraverse.traverse(node, new Visitor() {
			@Override
			public void visitHead(Node node) {
			}
			@Override
			public void visitNode(Node node) {
				node.setTs(node.getTs() + offset);
			}
			@Override
			public void visitLink(Link link, boolean hori) {
			}
		});
	}

	// Integer wrapper to make Java happy
	public static class Sum {
		public int size = 0;
	}

	public static int size(Node node) {
		final Sum sum = new Sum();
		ScanLineTraverse.traverse(node, new Visitor() {
			@Override
			public void visitHead(Node node) {
			}
			@Override
			public void visitNode(Node node) {
				sum.size++;
				for (int i = 0; i < node.links.length; i++) {
					if (node.links[i] != null)
						sum.size++;
				}
			}
			@Override
			public void visitLink(Link link, boolean hori) {
			}
		});
		return sum.size;
	}

	public static Graph toGraph(Node head) {
		Node clone = Ops.clone(head);
		return toGraphInPlace(clone);
	}

	public static Graph toGraphInPlace(Node head) {
		final Graph g = new Graph();
		ScanLineTraverse.traverse(head, new Visitor() {
			Long actor = -1L;
			@Override
			public void visitHead(Node node) {
				actor++;
			}
			@Override
			public void visitNode(Node node) {
				g.add(actor, node);
			}
			@Override
			public void visitLink(Link link, boolean hori) {
			}
		});
		return g;
	}

	public static String debug(Node node) {
		final StringBuilder str = new StringBuilder();
		ScanLineTraverse.traverse(node, new Visitor() {
			@Override
			public void visitHead(Node node) {
				str.append("visitHead " + node + "\n");
			}
			@Override
			public void visitNode(Node node) {
				str.append("visitNode " + node + "\n");
			}
			@Override
			public void visitLink(Link link, boolean hori) {
				str.append("visitLink " + link + "\n");
			}
		});
		return str.toString();
	}

	private static class ValidateState {
		boolean ok = true;
	}
	/**
	 * Check that timestamps increase monotonically
	 * @return
	 */
	public static boolean validate(Node node) {
		final ValidateState state = new ValidateState();
		ScanLineTraverse.traverse(node, new Visitor() {
			@Override
			public void visitHead(Node node) {
			}
			@Override
			public void visitNode(Node node) {
			}
			@Override
			public void visitLink(Link link, boolean hori) {
				long elapsed = link.to.getTs() - link.from.getTs();
				if (elapsed < 0) {
					state.ok = false;
					System.err.println("timestamps error on link " + link +
							" : " + link.from.getTs() + " -> " + link.to.getTs() +
							" (" + elapsed + ")");
				}
			}
		});
		return state.ok;
	}

	/**
	 * Check if traversing N1 and N2 yields the same node sequence,
	 * that all nodes have the same timestamps and that links type is the same.
	 * @param n1
	 * @param n2
	 * @return
	 */
	public static boolean match(Node n1, Node n2) {
		return match(n1, n2, MATCH_TIMESTAMPS | MATCH_LINKS_TYPE);
	}

	/**
	 * Check nodes and links structure
	 */
	public static final int MATCH_ISOMORPH = 0;

	/**
	 * Check all nodes timestamps for equality
	 */
	public static final int MATCH_TIMESTAMPS = 1 << 0;

	/**
	 * Check all links types for equality
	 */
	public static final int MATCH_LINKS_TYPE = 1 << 1;

	/**
	 * Check if traversing N1 and N2 respect properties PROP. Properties can be ORed.
	 * @param n1
	 * @param n2
	 * @param items
	 * @return
	 */
	public static boolean match(Node n1, Node n2, int prop) {
		Stack<Node> stack = new Stack<Node>();
		HashSet<Node> visited = new HashSet<Node>();
		stack.push(n2);
		stack.push(n1);
		boolean ok = true;
		while(!stack.isEmpty() && ok) {
			Node c1 = stack.pop();
			Node c2 = stack.pop();
			if (visited.contains(c1) && visited.contains(c2))
				continue;
			visited.add(c1);
			visited.add(c2);
			// check timestamps
			if ((prop & MATCH_TIMESTAMPS) != 0) {
				if (c1.compareTo(c2) != 0) {
					ok = false;
					break;
				}
			}
			// follow links, push next nodes
			for (int i = 0; i < c1.links.length; i++) {
				if (c1.hasNeighbor(i) && !c2.hasNeighbor(i) ||
					!c1.hasNeighbor(i) && c2.hasNeighbor(i)) {
					ok = false;
					break;
				} else {
					if (c1.hasNeighbor(i) && c2.hasNeighbor(i)) {
						// check links type
						if ((prop & MATCH_LINKS_TYPE) != 0) {
							if (c1.links[i].type != c2.links[i].type) {
								ok = false;
								break;
							}
						}
						stack.push(c2.neighbor(i));
						stack.push(c1.neighbor(i));
					}
				}
			}
		}
		return ok;
	}

}
