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
		x.linkHorizontal(y);
	}

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
				int s = sum.size;
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
		final Graph g = new Graph();
		FloodTraverse.traverse(head, new Visitor() {
			Long actor = -1L;
			@Override
			public void visitHead(Node node) {
				actor++;
			}
			@Override
			public void visitNode(Node node) {
				Link l1 = g.append(actor, new Node(node));
				Link l2 = node.links[Node.LEFT];
				if (l1 != null && l2 != null) {
					l1.type = l2.type;
				}
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

}
