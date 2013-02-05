package org.lttng.studio.reader.handler;

import java.util.HashMap;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDefinition;
import org.lttng.studio.model.graph.EdgeType;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecGraph;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.kernel.HRTimer;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.utils.AnalysisFilter;

public class TraceEventHandlerExecutionGraph  extends TraceEventHandlerBase {

	SystemModel system;
	ExecGraph graph;
	HRTimer[] hrtimerExpire;
	private AnalysisFilter filter;

	public TraceEventHandlerExecutionGraph() {
		super();
		hooks.add(new TraceHook("sched_process_fork"));
		hooks.add(new TraceHook("sched_process_exit"));
		hooks.add(new TraceHook("sched_wakeup"));
		hooks.add(new TraceHook("hrtimer_init"));
		hooks.add(new TraceHook("hrtimer_expire_entry"));
		hooks.add(new TraceHook("hrtimer_expire_exit"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		graph = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, ExecGraph.class);
		filter = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, AnalysisFilter.class);
		system.init(reader);
		hrtimerExpire = new HRTimer[reader.getNumCpus()];
	}

	public ExecVertex createVertex(Object owner, long timestamps) {
		ExecVertex vertex = new ExecVertex(owner, timestamps);
		graph.getVertexMap().put(owner, vertex);
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

	public void createSplit(Object source, Object target, long timestamps) {
		System.out.println("createSplit " + source + " -> " + target);
		/*
		 * v00 ---> v10
		 * 			||
		 *          \/
		 * v01 ---> v11
		 */

		ExecVertex v00 = graph.getEndVertexOf(source);
		ExecVertex v01 = graph.getEndVertexOf(target);
		ExecVertex v10 = createVertex(source, timestamps);
		ExecVertex v11 = createVertex(target, timestamps);

		createEdge(v00, v10, EdgeType.RUNNING);
		createEdge(v01, v11, EdgeType.DEFAULT);
		createEdge(v10, v11, EdgeType.DEFAULT);
	}

	public void createMerge(Object source, Object target, long timestamps) {
		System.out.println("createMerge " + source + " -> " + target);
		/*
		 * v00 ---> v10
		 * 			/\
		 *          ||
		 * v01 ---> v11
		 */

		ExecVertex v00 = graph.getEndVertexOf(target);
		ExecVertex v01 = graph.getEndVertexOf(source);
		ExecVertex v10 = createVertex(target, timestamps);
		ExecVertex v11 = createVertex(source, timestamps);

		createEdge(v00, v10, EdgeType.BLOCKED);
		createEdge(v01, v11, EdgeType.RUNNING);
		createEdge(v11, v10, EdgeType.DEFAULT);
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

		if (!filter.containsTaskTid(parent))
			return;
		createSplit(parent, child, timestamps);
	}

	public void handle_sched_process_exit(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		long timestamps = event.getTimestamp();
		IntegerDefinition tidDef = (IntegerDefinition) def.get("_tid");
		Task task = system.getTask(tidDef.getValue());

		if (task == null)
			return;
		if (!filter.containsTaskTid(task))
			return;

		// v0 ---> v1

		ExecVertex v0 = graph.getEndVertexOf(task);
		ExecVertex v1 = createVertex(task, timestamps);
		createEdge(v0, v1, EdgeType.RUNNING);
	}

	public void handle_sched_wakeup(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		long timestamps = event.getTimestamp();
		IntegerDefinition tidDef = (IntegerDefinition) def.get("_tid");
		Task target = system.getTask(tidDef.getValue());

		if (!filter.containsTaskTid(target))
			return;

		Object source = null;

		// 1 - hrtimer wakeup
		HRTimer timer = hrtimerExpire[event.getCPU()];
		if (timer != null) {
			System.out.println("timer wakeup " + target);
			source = timer;
		}

		// 2 - waitpid wakeup
		if (source == null) {
			Task current = system.getTaskCpu(event.getCPU());
			System.out.println("sched_wakeup emitted by " + current + " " + current.getProcessStatus() + " " + current.getExecutionMode());
			if (current.getExecutionMode() == Task.execution_mode.SYSCALL &&
					current.getProcessStatus() == Task.process_status.EXIT) {
				source = current;
			}
		}

		if (target == null || source == null) {
			System.err.println("ERROR: null wakeup endpoint: source " + source + " target " + target);
			return;
		}
		createMerge(source, target, timestamps);
	}

	public void handle_hrtimer_init(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		IntegerDefinition hrtimerField = (IntegerDefinition) def.get("_hrtimer");
		HRTimer timer = system.getHRTimers().get(hrtimerField.getValue());
		Task current = system.getTaskCpu(event.getCPU());
		if (current == null)
			return;
		if (!filter.containsTaskTid(current))
			return;
		createSplit(current, timer, event.getTimestamp());
	}

	public void handle_hrtimer_expire_entry(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		IntegerDefinition hrtimerField = (IntegerDefinition) def.get("_hrtimer");
		hrtimerExpire[event.getCPU()] = system.getHRTimers().get(hrtimerField.getValue());
	}

	public void handle_hrtimer_expire_exit(TraceReader reader, EventDefinition event) {
		hrtimerExpire[event.getCPU()] = null;
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

}
