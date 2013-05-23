package org.lttng.studio.model.zgraph.analysis;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.lttng.studio.model.zgraph.Graph;
import org.lttng.studio.model.zgraph.Link;
import org.lttng.studio.model.zgraph.LinkType;
import org.lttng.studio.model.zgraph.Node;

public class CriticalPathAlgorithmUnbounded extends AbstractCriticalPathAlgorithm {

	public CriticalPathAlgorithmUnbounded(Graph main) {
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
			case BLOCKED:
				List<Link> links = resolveBlockingUnbounded(link, start);
				Collections.reverse(links);
				System.out.println("links:");
				System.out.println(links);
				System.out.println(path.dump());
				stiches(path, link, links);
				System.out.println("after stiches:");
				System.out.println(path.dump());
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

	private void stiches(Graph path, Link blocking, List<Link> links) {
		Object master = getGraph().getParentOf(blocking.from);
		if (links.isEmpty()) {
			path.append(master, new Node(blocking.to)).type = LinkType.UNKNOWN;
			return;
		}
		// rewind path if required
		Link first = links.get(0);
		Node anchor = path.getTail(master);
		if (first.from.compareTo(anchor) < 0 && anchor.hasNeighbor(Node.LEFT)) {
			LinkType oldType = LinkType.UNKNOWN;
			while (first.from.compareTo(anchor) < 0 && anchor.hasNeighbor(Node.LEFT)) {
				anchor = path.removeTail(master);
				oldType = anchor.links[Node.LEFT].type;
			}
			anchor.links[Node.RIGHT] = null;
			Link tmp = path.append(master, anchor);
			if (tmp != null)
				tmp.type = oldType;
		}
		Object obj = getGraph().getParentOf(first.from);
		if (obj != master) {
			// fill any gap
			if (anchor.getTs() != first.from.getTs()) {
				anchor = new Node(first.from);
				path.append(master, anchor).type = LinkType.UNKNOWN;
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

	private List<Link> resolveBlockingUnbounded(Link blocking, Node bound) {
		List<Link> subPath = new LinkedList<Link>();
		Node junction = findIncoming(blocking.to, Node.RIGHT);
		// if wake-up source is not found, return empty list
		if (junction == null) {
			return subPath;
		}
		Link down = junction.links[Node.DOWN];
		subPath.add(down);
		Node node = down.from;
		while(node != null && node.compareTo(bound) > 0) {
			// prefer a path that converges
			if (node.hasNeighbor(Node.DOWN)) {
				Node conv = node.neighbor(Node.DOWN);
				Object parent = getGraph().getParentOf(conv);
				Object master = getGraph().getParentOf(bound);
				if (parent == master) {
					subPath.add(node.links[Node.DOWN]);
					break;
				}
			}
			if (node.hasNeighbor(Node.LEFT)) {
				Link link = node.links[Node.LEFT];
				if (link.type == LinkType.BLOCKED) {
					subPath.addAll(resolveBlockingUnbounded(link, bound));
				} else {
					subPath.add(link);
				}
			}
			node = node.left();
		}
		return subPath;
	}

}
