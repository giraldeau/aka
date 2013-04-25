package org.lttng.studio.reader.handler;

import java.util.Collection;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.lttng.studio.model.kernel.HRTimer;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.kernel.Task.thread_type;
import org.lttng.studio.model.zgraph.Graph;
import org.lttng.studio.model.zgraph.Link;
import org.lttng.studio.model.zgraph.LinkType;
import org.lttng.studio.model.zgraph.Node;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class TraceEventHandlerExecutionGraph  extends TraceEventHandlerBase {

	SystemModel system;
	Graph graph;
	HRTimer[] hrtimerExpire;
	private CtfTmfEvent[] softirq;
	CtfTmfEvent event;
	private ALog log;

	public TraceEventHandlerExecutionGraph() {
		super();
		hooks.add(new TraceHook("sched_switch"));
		hooks.add(new TraceHook("sched_process_exit"));
		hooks.add(new TraceHook("sched_wakeup"));
		hooks.add(new TraceHook("sched_wakeup_new"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
		graph = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, Graph.class);
		hrtimerExpire = new HRTimer[reader.getNumCpus()];
		softirq = new CtfTmfEvent[reader.getNumCpus()];
		log = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, ALog.class);
		log.message("init TraceEventHandlerExecutionGraph");
		// init graph
		Collection<Task> tasks = system.getTasks();
		for (Task task: tasks) {
			graph.add(task, new Node(task.getStart()));
		}
	}

	public void handle_sched_switch(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		long ts = event.getTimestamp().getValue();
		long prev_state = EventField.getLong(event, "prev_state");
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

		//process_status s1 = prevTask.getProcessStatusPrev();
		//process_status s2 = nextTask.getProcessStatusPrev();
		appendTaskNode(prevTask, ts);
		appendTaskNode(nextTask, ts);
	}

	public void appendTaskNode(Task task, long ts) {
		Link link = graph.append(task, new Node(ts));
		if (link != null) {
			link.type = LinkType.RUNNING;
		}
	}

	public void handle_sched_wakeup_new(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		long timestamps = event.getTimestamp().getValue();
		long childTid = EventField.getLong(event, "tid");
		Task parent = system.getTaskCpu(cpu);
		Task child = system.getTask(childTid);

		if (parent == null || child == null) {
			System.err.println("parent " + parent + " child " + child);
		}

		//createSplit(parent, child, timestamps);
	}

	public void handle_sched_process_exit(TraceReader reader, CtfTmfEvent event) {
		long timestamps = event.getTimestamp().getValue();
		long tid = EventField.getLong(event, "tid");
		Task task = system.getTask(tid);

		if (task == null)
			return;

		// v0 ---> v1

		//ExecVertex v0 = graph.getEndVertexOf(task);
		//ExecVertex v1 = createVertex(task, timestamps);
		//createEdge(v0, v1, EdgeType.RUNNING);
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

		// spurious wakeup
		if (target.getProcessStatus() != Task.process_status.WAIT_BLOCKED) {
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
			boolean isKernel = current.getThreadType() == thread_type.KERNEL_THREAD;
			boolean isSyscall = current.getExecutionMode() == Task.execution_mode.SYSCALL;
			boolean isExit = current.getProcessStatus() == Task.process_status.EXIT;
			boolean isRun = current.getProcessStatus() == Task.process_status.RUN;
			if ((isKernel && isRun) || (!isKernel && isSyscall && (isExit || isRun))){
				source = current;
			}
		}

		log.debug("sched_wakeup source=" + source + " target=" + target);
		if (target == null || source == null) {
			//System.err.println("WARNING: null wakeup endpoint: source:" + source + " target:" + target + " " + event.getTimestamp());
			return;
		}
		//createMerge(source, target, timestamps);
	}

	@Override
	public void handleComplete(TraceReader reader) {
		log.message("init TraceEventHandlerExecutionGraph");
	}

}
