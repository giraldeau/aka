package org.lttng.studio.model.zgraph.analysis;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.lttng.studio.model.zgraph.Graph;
import org.lttng.studio.model.zgraph.Link;
import org.lttng.studio.model.zgraph.LinkType;
import org.lttng.studio.model.zgraph.Node;

public class CriticalPathAlgorithmBounded extends AbstractCriticalPathAlgorithm {

	public CriticalPathAlgorithmBounded(Graph main) {
		super(main);
	}

	@Override
	public Graph compute(Node start, Node end) {
		Graph path = new Graph();
		if (start == null)
			return path;
		Object parent = getGraph().getParentOf(start);
		path.add(parent, new Node(start));
		Node curr = start;
		while(curr.hasNeighbor(Node.RIGHT)) {
			Node next = curr.neighbor(Node.RIGHT);
			Link link = curr.links[Node.RIGHT];
			switch(link.type) {
			case USER_INPUT:
			case BLOCK_DEVICE:
			case TIMER:
			case INTERRUPTED:
			case PREEMPTED:
			case RUNNING:
				path.append(getGraph().getParentOf(link.to), new Node(link.to)).type = link.type;
				break;
			case NETWORK:
			case BLOCKED:
				List<Link> links = resolveBlockingBounded(link, link.from);
				Collections.reverse(links);
				glue(path, curr, links);
				break;
			case EPS:
				if (link.duration() != 0)
					throw new RuntimeException("epsilon duration is not zero " + link);
				break;
			case DEFAULT:
				throw new RuntimeException("Illegal link type " + link.type);
			default:
				break;
			}
			curr = next;
		}
		return path;
	}

	private void glue(Graph path, Node curr, List<Link> links) {
		Object currentActor = getGraph().getParentOf(curr);
		if (links.isEmpty()) {
			Node next = curr.neighbor(Node.RIGHT);
			path.append(currentActor, new Node(next)).type = curr.links[Node.RIGHT].type;
			return;
		}
		// FIXME: assert last link.to actor == currentActor

		// attach subpath to b1 and b2
		Node b1 = path.getTail(currentActor);
		Node b2 = new Node(curr.neighbor(Node.RIGHT));
		Node anchor;

		// glue head
		Link lnk = links.get(0);
		Object objSrc = getGraph().getParentOf(lnk.from);
		if (objSrc == currentActor) {
			anchor = b1;
		} else {
			anchor = new Node(curr);
			path.add(objSrc, anchor);
			b1.linkVertical(anchor);
			// fill any gap with UNKNOWN
			if (lnk.from.compareTo(anchor) > 0) {
				anchor = new Node(lnk.from);
				path.append(objSrc, anchor).type = LinkType.UNKNOWN;
			}
		}

		// glue body
		Link prev = null;
		for (Link link: links) {
			// check connectivity
			if (prev != null && prev.to != link.from) {
				anchor = copyLink(path, anchor, prev.to, link.from,
						prev.to.getTs(), LinkType.DEFAULT);
			}
			anchor = copyLink(path, anchor, link.from, link.to,
					link.to.getTs(), link.type);
			prev = link;
		}
	}

	// FIXME: build a tree with partial subpath in order to return the best path,
	// not the last one traversed
	private List<Link> resolveBlockingBounded(Link blocking, Node bound) {
		LinkedList<Link> subPath = new LinkedList<Link>();
		Node junction = findIncoming(blocking.to, Node.RIGHT);
		// if wake-up source is not found, return empty list
		if (junction == null) {
			return subPath;
		}
		Link down = junction.links[Node.DOWN];
		subPath.add(down);
		Node node = down.from;
		bound = bound.compareTo(blocking.from) < 0 ? blocking.from : bound;
		Stack<Node> stack = new Stack<Node>();
		while(node != null && node.compareTo(bound) > 0) {
			// shortcut for down link that goes beyond the blocking
			if (node.hasNeighbor(Node.DOWN) && node.down().compareTo(bound) <= 0) {
				subPath.add(node.links[Node.DOWN]);
				break;
			}

			/*
			 * Add DOWN links to explore stack in case dead-end occurs
			 * Do not add if left is BLOCKED, because this link would be visited twice
			 */
			if (node.hasNeighbor(Node.DOWN) &&
					(!node.hasNeighbor(Node.LEFT) || (node.hasNeighbor(Node.LEFT)
					&& (node.links[Node.LEFT].type != LinkType.BLOCKED ||
						node.links[Node.LEFT].type != LinkType.NETWORK)))) {
				stack.push(node);
			}
			if (node.hasNeighbor(Node.LEFT)) {
				Link link = node.links[Node.LEFT];
				if (link.type == LinkType.BLOCKED || link.type == LinkType.NETWORK) {
					subPath.addAll(resolveBlockingBounded(link, bound));
				} else {
					subPath.add(link);
				}
			} else {
				if (!stack.isEmpty()) {
					Node n = stack.pop();
					// rewind subpath
					while(!subPath.isEmpty() && subPath.getLast().from != n) {
						subPath.removeLast();
					}
					subPath.add(n.links[Node.DOWN]);
					node = n.neighbor(Node.DOWN);
					continue;
				}
			}
			node = node.left();
		}
		return subPath;
	}

}
