package org.lttng.studio.model.zgraph;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CriticalPath {

	private final Graph main;

	public CriticalPath(Graph main) {
		this.main = main;
	}

	public Graph criticalPathBounded(Node start) {
		Graph path = new Graph();
		Object parent = main.getParentOf(start);
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
				path.append(main.getParentOf(link.to), new Node(link.to)).type = link.type;
				break;
			case BLOCKED:
				List<Link> links = resolveBlocking(link, link.from);
				Collections.reverse(links);
				glue(path, curr, links);
				break;
			case EPS:
				if (link.duration() != 0)
					throw new RuntimeException("epsilon duration is not zero " + link);
				break;
			case DEFAULT:
			case NETWORK:
				throw new RuntimeException("Illegal link type " + link.type);
			default:
				break;
			}
			curr = next;
		}
		return path;
	}

	private void glue(Graph path, Node curr, List<Link> links) {
		Object currentActor = main.getParentOf(curr);
		if (links.isEmpty()) {
			Node next = curr.neighbor(Node.RIGHT);
			path.append(currentActor, new Node(next)).type = LinkType.UNKNOWN;
			return;
		}
		// FIXME: assert last link.to actor == currentActor

		// attach subpath to b1 and b2
		Node b1 = path.getTail(currentActor);
		Node b2 = new Node(curr.neighbor(Node.RIGHT));
		Node anchor;

		// glue head
		Link lnk = links.get(0);
		Object objSrc = main.getParentOf(lnk.from);
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

	/**
	 * Copy link of type TYPE between nodes FROM and TO in the graph PATH.
	 * The return value is the tail node for the new path.
	 * @param path
	 * @param anchor
	 * @param from
	 * @param to
	 * @param type
	 * @return
	 */
	public Node copyLink(Graph path, Node anchor, Node from, Node to, long ts, LinkType type) {
		Object parentFrom = main.getParentOf(from);
		Object parentTo = main.getParentOf(to);
		Node tmp = new Node(ts);
		path.add(parentTo, tmp);
		if (parentFrom == parentTo) {
			anchor.linkHorizontal(tmp).type = type;
		} else {
			anchor.linkVertical(tmp).type = type;
		}
		return tmp;
	}

	private List<Link> resolveBlocking(Link blocking, Node bound) {
		List<Link> subPath = new LinkedList<Link>();
		Node junction = findIncoming(blocking.to, Node.RIGHT);
		// if wake-up source is not found, return empty list
		if (junction == null) {
			return subPath;
		}
		Link down = junction.links[Node.DOWN];
		subPath.add(down);
		Node node = down.from;
		bound = bound.compareTo(blocking.from) < 0 ? blocking.from : bound;
		while(node != null && node.compareTo(bound) > 0) {
			if (node.hasNeighbor(Node.DOWN) && node.down().compareTo(bound) <= 0) {
				subPath.add(node.links[Node.DOWN]);
				break;
			}
			if (node.hasNeighbor(Node.LEFT)) {
				Link link = node.links[Node.LEFT];
				if (link.type == LinkType.BLOCKED) {
					subPath.addAll(resolveBlocking(link, bound));
				} else {
					subPath.add(link);
				}
			}
			node = node.left();
		}
		return subPath;
	}

	public static Node findIncoming(Node node, int dir) {
		while(true) {
			if (node.hasNeighbor(Node.DOWN))
				return node;
			if (!(node.hasNeighbor(dir) &&
					node.links[dir].type == LinkType.EPS)) {
				break;
			}
			node = node.neighbor(dir);
		}
		return null;
	}

}