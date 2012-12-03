package org.lttng.studio.reader.handler;

import java.util.HashMap;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDefinition;
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

	public void handle_sched_process_fork(TraceReader reader, EventDefinition event) {
		// TODO: add child to parent's children list
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		IntegerDefinition parentTidDef = (IntegerDefinition) def.get("_parent_tid");
		IntegerDefinition childTidDef = (IntegerDefinition) def.get("_child_tid");
		//ArrayDefinition name = (ArrayDefinition) def.get("_child_comm");
		Task parent = system.getTask(parentTidDef.getValue());
		Task child = system.getTask(childTidDef.getValue());

		if (parent == null || child == null) {
			System.err.println("parent " + parent + " child " + child);
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
