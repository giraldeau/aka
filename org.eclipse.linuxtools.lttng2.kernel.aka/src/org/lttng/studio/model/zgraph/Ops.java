package org.lttng.studio.model.zgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

public class Ops {

	private interface Visitor {
		public void visitHead(Node node);
		public void visitNode(Node node);
		public void visitLink(Link link, boolean horizontal);
	}

	private static class FloodTraverse {
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
		FloodTraverse.traverse(orig, new Visitor() {
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
		FloodTraverse.traverse(orig, new Visitor() {
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
		Node h1 = Ops.head(n1);
		Node h2 = Ops.head(n2);
		Node t1 = Ops.tail(n1);
		Node t2 = Ops.tail(n2);
		// add epsilon nodes to prevent overwrite of existing links
		h1 = Ops.epsilon(h1, Node.LEFT);
		h2 = Ops.epsilon(h2, Node.LEFT);
		t1 = Ops.epsilon(t1, Node.RIGHT);
		t2 = Ops.epsilon(t2, Node.RIGHT);
		Link l1 = h1.linkVertical(h2);
		Link l2 = t2.linkVertical(t1);
		l1.type = split;
		l2.type = merge;
		return h1;
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

	public static Node superimpose() {
		return null;
	}

	public static Node minimize() {
		return null;

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
		FloodTraverse.traverse(node, new Visitor() {
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
		FloodTraverse.traverse(node, new Visitor() {
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
		FloodTraverse.traverse(head, new Visitor() {
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
		FloodTraverse.traverse(node, new Visitor() {
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
		FloodTraverse.traverse(node, new Visitor() {
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

}
