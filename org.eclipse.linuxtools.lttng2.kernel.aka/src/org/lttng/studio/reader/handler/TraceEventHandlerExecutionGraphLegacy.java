package org.lttng.studio.reader.handler;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.lttng.studio.model.graph.EdgeType;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecGraph;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.kernel.HRTimer;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.kernel.Task.thread_type_enum;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class TraceEventHandlerExecutionGraphLegacy  extends TraceEventHandlerBase {

	SystemModel system;
	ExecGraph graph;
	HRTimer[] hrtimerExpire;
	private CtfTmfEvent[] softirq;
	CtfTmfEvent event;
	private ALog log;

	public TraceEventHandlerExecutionGraphLegacy() {
		super();
		hooks.add(new TraceHook("sched_switch"));
		hooks.add(new TraceHook("sched_process_fork"));
		hooks.add(new TraceHook("sched_process_exit"));
		hooks.add(new TraceHook("sched_wakeup"));
		hooks.add(new TraceHook("hrtimer_init"));
		hooks.add(new TraceHook("hrtimer_expire_entry"));
		hooks.add(new TraceHook("hrtimer_expire_exit"));
		hooks.add(new TraceHook("softirq_entry"));
		hooks.add(new TraceHook("softirq_exit"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		graph = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, ExecGraph.class);
		system.init(reader);
		hrtimerExpire = new HRTimer[reader.getNumCpus()];
		softirq = new CtfTmfEvent[reader.getNumCpus()];
		log = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, ALog.class);
		//log.message("init TraceEventHandlerExecutionGraph");
		log = new ALog();
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
		if (edge != null)
			log.debug("createEdge " + edge + " " + edge.getType());
		return edge;
	}

	public void createSplit(Object source, Object target, long timestamps, EdgeType prevType) {
		log.debug("createSplit " + source + " -> " + target);
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
		createEdge(v01, v11, prevType);
		createEdge(v10, v11, EdgeType.SPLIT);
	}

	public void createSplit(Object source, Object target, long timestamps) {
		createSplit(source, target, timestamps, EdgeType.DEFAULT);
	}

	public void createMerge(Object source, Object target, long timestamps) {
		log.debug("createMerge " + source + " -> " + target);
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
		createEdge(v11, v10, EdgeType.MERGE);
	}

	public void handle_sched_switch(TraceReader reader, CtfTmfEvent event) {
		//int cpu = event.getCPU();
		//long prev_state = EventField.getLong(event, "prev_state");
		long next = EventField.getLong(event, "next_tid");
		long prev = EventField.getLong(event, "prev_tid");

		Task nextTask = system.getTask(next);
		Task prevTask = system.getTask(prev);

		if (prevTask == null || nextTask == null) {
			log.warning("prevTask=" + prevTask + " nextTask=" + nextTask);
			return;
		}
		log.debug(String.format("%5d %12s %12s",
				prevTask.getTid(),
				prevTask.getProcessStatusPrev(),
				prevTask.getProcessStatus()));
		log.debug(String.format("%5d %12s %12s",
				nextTask.getTid(),
				nextTask.getProcessStatusPrev(),
				nextTask.getProcessStatus()));

		/*
		process_status status = task.getProcessStatus();
		if (status != process_status.RUN && status != process_status.EXIT) {
			System.out.println("WARNING: prev task was not running " + task + " " + task.getProcessStatus() + " " + event.getTimestamp());
		}
		// prev_state == 0 means runnable, thus waits for cpu
		graph.getEndVertexOf();

		if (prev_state == 0) {
			createEdge
			_update_task_state(prev, process_status.WAIT_CPU);
		} else {
			_update_task_state(prev, process_status.WAIT_BLOCKED);
		}
		*/
	}

	public void handle_sched_process_fork(TraceReader reader, CtfTmfEvent event) {
		long timestamps = event.getTimestamp().getValue();
		long parentTid = EventField.getLong(event, "parent_tid");
		long childTid = EventField.getLong(event, "child_tid");
		Task parent = system.getTask(parentTid);
		Task child = system.getTask(childTid);

		if (parent == null || child == null) {
			System.err.println("parent " + parent + " child " + child);
			return;
		}

		//if (!filter.containsTaskTid(parent))
		//	return;
		createSplit(parent, child, timestamps);
	}

	public void handle_sched_process_exit(TraceReader reader, CtfTmfEvent event) {
		long timestamps = event.getTimestamp().getValue();
		long tid = EventField.getLong(event, "tid");
		Task task = system.getTask(tid);

		if (task == null)
			return;
		//if (!filter.containsTaskTid(task))
		//	return;

		// v0 ---> v1

		ExecVertex v0 = graph.getEndVertexOf(task);
		ExecVertex v1 = createVertex(task, timestamps);
		createEdge(v0, v1, EdgeType.RUNNING);
	}

	public void handle_sched_wakeup(TraceReader reader, CtfTmfEvent event) {
		long timestamps = event.getTimestamp().getValue();
		long tid = EventField.getLong(event, "tid");
		Task target = system.getTask(tid);
		Task current = system.getTaskCpu(event.getCPU());
		if (current == null || target == null) {
			log.warning("wakeup current=" + current  + " target=" + target);
			return;
		}

		//if (!filter.containsTaskTid(target))
		//	return;

		// spurious wakeup
		if (target.getProcessStatusPrev() != Task.process_status_enum.WAIT_BLOCKED) {
			log.debug("sched_wakeup target " + target + " is not in WAIT_BLOCKED: " + target.getProcessStatus());
			return;
		}

		Object source = null;

		// 1 - hrtimer wakeup
		HRTimer timer = hrtimerExpire[event.getCPU()];
		if (timer != null) {
			//System.out.println("timer wakeup " + target);
			source = timer;
		}

		// 2 - softirq wakeup
		CtfTmfEvent sirq = softirq[event.getCPU()];
		if (sirq != null) {
			// lookup the source of this SoftIRQ
			return;
		}

		// 3 - waitpid wakeup
		if (source == null) {
			boolean isKernel = current.getThreadType() == thread_type_enum.KERNEL_THREAD;
			boolean isSyscall = current.getExecutionMode() == Task.execution_mode_enum.SYSCALL;
			boolean isExit = current.getProcessStatus() == Task.process_status_enum.EXIT;
			boolean isRun = current.getProcessStatus() == Task.process_status_enum.RUN;
			if ((isKernel && isRun) || (!isKernel && isSyscall && (isExit || isRun))){
				source = current;
			}
		}

		log.debug("sched_wakeup source=" + source + " target=" + target);
		if (target == null || source == null) {
			//System.err.println("WARNING: null wakeup endpoint: source:" + source + " target:" + target + " " + event.getTimestamp());
			return;
		}
		createMerge(source, target, timestamps);
	}

	public void handle_hrtimer_init(TraceReader reader, CtfTmfEvent event) {
		long  hrtimer = EventField.getLong(event, "hrtimer");
		HRTimer timer = system.getHRTimers().get(hrtimer);
		Task current = system.getTaskCpu(event.getCPU());
		if (current == null)
			return;
		//if (!filter.containsTaskTid(current))
		//	return;
		createSplit(current, timer, event.getTimestamp().getValue(), EdgeType.BLOCKED);
	}

	public void handle_hrtimer_expire_entry(TraceReader reader, CtfTmfEvent event) {
		long hrtimer = EventField.getLong(event, "hrtimer");
		hrtimerExpire[event.getCPU()] = system.getHRTimers().get(hrtimer);
	}

	public void handle_hrtimer_expire_exit(TraceReader reader, CtfTmfEvent event) {
		hrtimerExpire[event.getCPU()] = null;
	}

	public void handle_softirq_entry(TraceReader reader, CtfTmfEvent event) {
		softirq[event.getCPU()] = event;
	}

	public void handle_softirq_exit(TraceReader reader, CtfTmfEvent event) {
		softirq[event.getCPU()] = null;
	}

	@Override
	public void handleComplete(TraceReader reader) {
		log.message("init TraceEventHandlerExecutionGraph");
	}

}
