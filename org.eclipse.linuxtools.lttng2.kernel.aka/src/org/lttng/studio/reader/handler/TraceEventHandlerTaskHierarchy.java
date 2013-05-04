package org.lttng.studio.reader.handler;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.lttng.studio.model.graph.TaskHierarchyGraph;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class TraceEventHandlerTaskHierarchy  extends TraceEventHandlerBase {

	SystemModel system;
	TaskHierarchyGraph graph;

	public TraceEventHandlerTaskHierarchy() {
		super();
		hooks.add(new TraceHook("sched_process_fork"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		graph = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, TaskHierarchyGraph.class);
		system.init(reader);
	}

	public void handle_sched_process_fork(TraceReader reader, CtfTmfEvent event) {
		// TODO: add child to parent's children list
		long parentTid = EventField.getLong(event, "parent_tid");
		long childTid = EventField.getLong(event, "child_tid");
		Task parent = system.getTask(parentTid);
		Task child = system.getTask(childTid);

		if (parent == null || child == null) {
			//System.err.println("parent " + parent + " child " + child);
			return;
		}

		DirectedGraph<Task,DefaultEdge> directedGraph = graph.getGraph();

		if (!directedGraph.containsVertex(parent))
			directedGraph.addVertex(parent);
		if (!directedGraph.containsVertex(child))
			directedGraph.addVertex(child);
		directedGraph.addEdge(parent, child);
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

}
