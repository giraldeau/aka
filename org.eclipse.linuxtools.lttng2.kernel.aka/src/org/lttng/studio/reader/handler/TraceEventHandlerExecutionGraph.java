package org.lttng.studio.reader.handler;

import java.util.Collection;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.lttng.studio.model.kernel.HRTimer;
import org.lttng.studio.model.kernel.InterruptContext;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.kernel.Task.process_status_enum;
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
		long ts = event.getTimestamp().getValue();
		long next = EventField.getLong(event, "next_tid");
		long prev = EventField.getLong(event, "prev_tid");

		Task nextTask = system.getTask(next);
		Task prevTask = system.getTask(prev);

		if (prevTask == null || nextTask == null) {
			log.warning("prevTask=" + prevTask + " nextTask=" + nextTask);
			return;
		}
		log.debug(event.getTimestamp().toString());
		log.debug(String.format("%5d %12s %12s",
				prevTask.getTid(),
				prevTask.getProcessStatusPrev(),
				prevTask.getProcessStatus()));
		log.debug(String.format("%5d %12s %12s",
				nextTask.getTid(),
				nextTask.getProcessStatusPrev(),
				nextTask.getProcessStatus()));

		appendTaskNode(prevTask, ts);
		appendTaskNode(nextTask, ts);
	}

	public void appendTaskNode(Task task, long ts) {
		Link link = graph.append(task, new Node(ts));
		if (link != null) {
			process_status_enum status = task.getProcessStatusPrev();
			switch(status) {
			case DEAD:
				break;
			case EXIT:
			case RUN:
				link.type = LinkType.RUNNING;
				break;
			case UNNAMED:
				link.type = LinkType.UNKNOWN;
				break;
			case WAIT_BLOCKED:
				link.type = LinkType.BLOCKED;
				break;
			case WAIT_CPU:
			case WAIT_FORK:
				link.type = LinkType.PREEMPTED;
				break;
			case ZOMBIE:
				link.type = LinkType.UNKNOWN;
				break;
			default:
				break;

			}
		}
	}

	public void handle_sched_wakeup_new(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		long ts = event.getTimestamp().getValue();
		long childTid = EventField.getLong(event, "tid");
		Task parent = system.getTaskCpu(cpu);
		Task child = system.getTask(childTid);

		if (parent == null || child == null) {
			System.err.println("parent " + parent + " child " + child);
		}
		Node n0 = new Node(ts);
		Node n1 = new Node(ts);
		Link l0 = graph.append(parent, n0);
		if (l0 != null)
			l0.type = LinkType.RUNNING;
		graph.append(child, n1);
		n0.linkVertical(n1).type = LinkType.DEFAULT;
	}


	/*
	0 HI: hi priority tasklet
	1 TIMER
	2 NET_TX
	3 NET_RX
	4 BLOCK
	5 BLOCK_IOPOLL
	6 TASKLET
	7 SCHED
	8 HRTIMER
	9 RCU
	 */
	public void handle_sched_wakeup(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		long ts = event.getTimestamp().getValue();
		long tid = EventField.getLong(event, "tid");
		Task target = system.getTask(tid);
		Task current = system.getTaskCpu(event.getCPU());
		if (current == null || target == null) {
			log.warning("wakeup current=" + current  + " target=" + target);
			return;
		}

		// spurious wakeup
		if (target.getProcessStatus() != Task.process_status_enum.WAIT_BLOCKED) {
			log.debug("sched_wakeup target " + target + " is not in WAIT_BLOCKED: " + target.getProcessStatus());
			return;
		}

		InterruptContext context = system.getInterruptContext(cpu).peek();
		switch (context.getContext()) {
		case HRTIMER:
			Link l1 = graph.append(target, new Node(ts));
			if (l1 != null) {
				l1.type = LinkType.TIMER;
			}
			break;
		case IRQ:
			break;
		case SOFTIRQ:
			Link l2 = graph.append(target, new Node(ts));
			if (l2 != null) {
				CtfTmfEvent softirqEv = context.getEvent();
				long vec = EventField.getLong(softirqEv, "vec");
				if (vec == 1) {
					l2.type = LinkType.TIMER;
				} else {
					l2.type = LinkType.UNKNOWN;
				}
			}
			break;
		case NONE:

			break;
		default:
			break;
		}

	}

	@Override
	public void handleComplete(TraceReader reader) {
		log.message("init TraceEventHandlerExecutionGraph");
	}

}
