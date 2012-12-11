package org.lttng.studio.model.graph;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DirectedSubgraph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.Subgraph;
import org.jgrapht.traverse.ClosestFirstIterator;

/*
 * Compute the critical path between start and end vertex. ReverseClosestIterator
 * is used to iterates from the end to the start.
 */
public class TaskGraphExtractor {

	/*
	 * Returns the subgraph including all related vertex of the task,
	 * with edges on the critical path annotated
	 */
	public static Subgraph<ExecVertex, ExecEdge, DirectedGraph<ExecVertex, ExecEdge>>
	getExecutionGraph(ExecGraph graph, ExecVertex start, ExecVertex end) {
		final Set<ExecVertex> v1 = new HashSet<ExecVertex>();
		final Set<ExecEdge> e1 = new HashSet<ExecEdge>();
		final Set<ExecVertex> v2 = new HashSet<ExecVertex>();
		final Set<ExecEdge> e2 = new HashSet<ExecEdge>();

		EdgeReversedGraph<ExecVertex, ExecEdge> revGraph = new EdgeReversedGraph<ExecVertex, ExecEdge>(graph.getGraph());

		ClosestFirstIterator<ExecVertex, ExecEdge> iterator1 = new ClosestFirstIterator<ExecVertex, ExecEdge>(revGraph, end);
		ClosestFirstIterator<ExecVertex, ExecEdge> iterator2 = new ClosestFirstIterator<ExecVertex, ExecEdge>(graph.getGraph(), start);

		InventoryTraversalListener<ExecVertex, ExecEdge> inventoryReverse = new InventoryTraversalListener<ExecVertex, ExecEdge>(v1, e1);
		InventoryTraversalListener<ExecVertex, ExecEdge> inventoryForward = new InventoryTraversalListener<ExecVertex, ExecEdge>(v2, e2);

		iterator1.addTraversalListener(inventoryReverse);
		while(iterator1.hasNext()) {
			iterator1.next();
		}

		iterator2.addTraversalListener(inventoryForward);
		while(iterator2.hasNext()) {
			iterator2.next();
		}

		// union of sets
		v1.retainAll(v2);
		e1.retainAll(e2);
		Subgraph<ExecVertex, ExecEdge, DirectedGraph<ExecVertex, ExecEdge>> sub =
				new DirectedSubgraph<ExecVertex, ExecEdge>(graph.getGraph(), v1, e1);

		// FIXME: annotate the critical path

		return sub;
	}

}
