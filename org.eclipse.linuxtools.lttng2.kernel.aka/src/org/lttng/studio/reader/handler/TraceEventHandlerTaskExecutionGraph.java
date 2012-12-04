package org.lttng.studio.reader.handler;

import java.util.HashMap;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDefinition;
import org.lttng.studio.model.graph.EdgeType;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.graph.TaskExecutionGraph;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class TraceEventHandlerTaskExecutionGraph  extends TraceEventHandlerBase {

	SystemModel system;
	TaskExecutionGraph graph;
	//HashMap<Object, ExecVertex> tailMap;

	public TraceEventHandlerTaskExecutionGraph() {
		super();
		hooks.add(new TraceHook("sched_process_fork"));
		hooks.add(new TraceHook("sched_process_exit"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		graph = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, TaskExecutionGraph.class);
		system.init(reader);
	}

	public ExecVertex createVertex(Task task, long timestamps) {
		ExecVertex vertex = new ExecVertex(task, timestamps, false);
		graph.getVertexMap().put(task, vertex);
		graph.getGraph().addVertex(vertex);
		return vertex;
	}

	public ExecEdge createEdge(ExecVertex node, ExecVertex next, EdgeType type) {
		ExecEdge edge = null;
		if (node != null && next != null) {
			edge = graph.getGraph().addEdge(node, next);
			edge.setType(type);
		}
		return edge;
	}

	public void handle_sched_process_fork(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		long timestamps = event.getTimestamp();
		IntegerDefinition parentTidDef = (IntegerDefinition) def.get("_parent_tid");
		IntegerDefinition childTidDef = (IntegerDefinition) def.get("_child_tid");
		Task parent = system.getTask(parentTidDef.getValue());
		Task child = system.getTask(childTidDef.getValue());

		if (parent == null || child == null) {
			System.err.println("parent " + parent + " child " + child);
		}

		/*
		 * v00 ---> v10
		 * 			||
		 *          \/
		 * v01 ---> v11
		 */

		ExecVertex v00 = graph.getStartVertexOf(parent);
		ExecVertex v01 = graph.getStartVertexOf(child);
		ExecVertex v10 = createVertex(parent, timestamps);
		ExecVertex v11 = createVertex(child, timestamps);

		createEdge(v00, v10, EdgeType.RUNNING);
		createEdge(v01, v11, EdgeType.UNKOWN);
		createEdge(v10, v11, EdgeType.MSG_TASK_FORK);
	}

	public void handle_sched_process_exit(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		long timestamps = event.getTimestamp();
		IntegerDefinition tidDef = (IntegerDefinition) def.get("_tid");
		Task task = system.getTask(tidDef.getValue());
		if (task == null)
			return;

		// v0 ---> v1

		ExecVertex v0 = graph.getEndVertexOf(task);
		ExecVertex v1 = createVertex(task, timestamps);
		createEdge(v0, v1, EdgeType.RUNNING);
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

}
