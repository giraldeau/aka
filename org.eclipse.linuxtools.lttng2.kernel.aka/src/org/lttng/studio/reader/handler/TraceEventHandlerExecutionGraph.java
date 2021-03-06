package org.lttng.studio.reader.handler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Stack;

import org.eclipse.linuxtools.lttng2.kernel.core.event.matching.TcpEventMatching;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.event.matching.IMatchProcessingUnit;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfEventDependency;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.lttng.studio.model.kernel.InterruptContext;
import org.lttng.studio.model.kernel.InterruptContext.Context;
import org.lttng.studio.model.kernel.Softirq;
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
	Task[] kernel;
	Graph graph;
	CtfTmfEvent event;
	private ALog log;
	IMatchProcessingUnit matchProcessing;
	HashMap<CtfTmfEvent, Node> tcpNodes;
	TcpEventMatching tcpMatching;

	public TraceEventHandlerExecutionGraph() {
		super();
		hooks.add(new TraceHook("sched_switch"));
		hooks.add(new TraceHook("sched_wakeup"));
		hooks.add(new TraceHook("sched_wakeup_new"));
		hooks.add(new TraceHook("softirq_entry"));
		hooks.add(new TraceHook("inet_sock_local_in"));
		hooks.add(new TraceHook("inet_sock_local_out"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
		graph = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, Graph.class);
		log = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, ALog.class);
		log.message("init TraceEventHandlerExecutionGraph");
		kernel = new Task[reader.getNumCpus()];
		for (int i = 0; i < reader.getNumCpus(); i++) {
			Task k = new Task();
			k.setName("kernel/" + i);
			k.setProcessStatus(process_status_enum.RUN);
			k.setProcessStatus(process_status_enum.RUN);
			k.setPid(-1);
			k.setTid(-1);
			k.setPpid(-1);
			kernel[i] = k;
		}
		// init graph
		Collection<Task> tasks = system.getTasks();
		for (Task task: tasks) {
			graph.add(task, new Node(task.getStart()));
		}
		tcpNodes = new HashMap<CtfTmfEvent, Node>();
		matchProcessing = new IMatchProcessingUnit() {

			@Override
			public void matchingEnded() {
			}

			@Override
			public void init(ITmfTrace[] fTraces) {
			}

			@Override
			public int countMatches() {
				return 0;
			}

			@Override
			public void addMatch(TmfEventDependency match) {
				log.debug("we got a tcp match! " + match.getSourceEvent().getContent() + " " + match.getDestinationEvent().getContent());
				Node output = tcpNodes.remove(match.getSourceEvent());
				Node input = tcpNodes.remove(match.getDestinationEvent());
				if (output == null || input == null) {
					log.warning("null tcp endpoint output=" + " input=" + input);
				} else {
					output.linkVertical(input).type = LinkType.NETWORK;
				}
			}
		};

		tcpMatching = new TcpEventMatching(reader.getTrace(), matchProcessing);
		tcpMatching.initMatching();
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
		stateChange(prevTask, ts);
		stateChange(nextTask, ts);
	}


	public Node stateExtend(Task task, long ts) {
		Node node = new Node(ts);
		Link link = graph.append(task, node);
		if (link != null) {
			process_status_enum status = task.getProcessStatus();
			link.type = resolveProcessStatus(status);
		}
		return node;
	}

	public Node stateChange(Task task, long ts) {
		Node node = new Node(ts);
		Link link = graph.append(task, node);
		if (link != null) {
			process_status_enum status = task.getProcessStatusPrev();
			link.type = resolveProcessStatus(status);
		}
		return node;
	}

	private LinkType resolveProcessStatus(process_status_enum status) {
		LinkType ret = LinkType.UNKNOWN;
		if (status == null)
			return ret;
		switch(status) {
		case DEAD:
			break;
		case EXIT:
		case RUN:
			ret = LinkType.RUNNING;
			break;
		case UNNAMED:
			ret = LinkType.UNKNOWN;
			break;
		case WAIT_BLOCKED:
			ret = LinkType.BLOCKED;
			break;
		case WAIT_CPU:
		case WAIT_FORK:
			ret = LinkType.PREEMPTED;
			break;
		case ZOMBIE:
			ret = LinkType.UNKNOWN;
			break;
		default:
			break;
		}
		return ret;
	}

	public void handle_sched_wakeup_new(TraceReader reader, CtfTmfEvent event) {
		long ts = event.getTimestamp().getValue();
		long tid = EventField.getLong(event, "tid");
		Task child = system.getTask(tid);
		Task parent = system.getTaskCpu(event.getCPU());
		if (child == null || parent == null) {
			log.warning("wakeup_new parent=" + parent + " child=" + child);
			return;
		}
		Node n0 = stateExtend(parent, ts);
		Node n1 = stateChange(child, ts);
		n0.linkVertical(n1);
	}

	public void handle_sched_wakeup(TraceReader reader, CtfTmfEvent event) {
		int cpu = event.getCPU();
		long ts = event.getTimestamp().getValue();
		long tid = EventField.getLong(event, "tid");
		Task target = system.getTask(tid);
		Task current = system.getTaskCpu(event.getCPU());
		if (target == null) {
			log.warning("wakeup current=" + current  + " target=" + target);
			return;
		}

		// spurious wakeup
		process_status_enum status = target.getProcessStatusPrev();
		if (status != process_status_enum.WAIT_BLOCKED) {
			log.debug("sched_wakeup target " + target + " is not in WAIT_BLOCKED: " + target.getProcessStatusPrev());
			return;
		}

		InterruptContext context = system.getInterruptContext(cpu).peek();
		switch (context.getContext()) {
		case HRTIMER:
			// shortcut of appendTaskNode: resolve blocking source in situ
			Link l1 = graph.append(target, new Node(ts));
			if (l1 != null) {
				l1.type = LinkType.TIMER;
			}
			break;
		case IRQ:
			Link l3 = graph.append(target, new Node(ts));
			if (l3 != null) {
				l3.type = resolveIRQ(context.getEvent());
			}
			break;
		case SOFTIRQ:
			Node wup = new Node(ts);
			Link l2 = graph.append(target, wup);
			if (l2 != null) {
				l2.type = resolveSoftirq(context.getEvent());
			}
			// special case for network related softirq
			int vec = (int) EventField.getLong(context.getEvent(), "vec");
			if (vec == Softirq.NET_RX || vec == Softirq.NET_TX) {
				Task k = kernel[event.getCPU()];
				Node kwup = stateExtend(k, event.getTimestamp().getValue());
				kwup.linkVertical(wup);
			}
			break;
		case NONE:
			// task context wakeup
			if (current != null) {
				Node n0 = stateExtend(current, ts);
				Node n1 = stateChange(target, ts);
				n0.linkVertical(n1);
			} else {
				stateChange(target, ts);
			}
			break;
		default:
			break;
		}

	}

	private LinkType resolveIRQ(CtfTmfEvent event) {
		int vec = (int) EventField.getLong(event, "irq");
		LinkType ret = LinkType.UNKNOWN;
		switch(vec) {
		case 0: // resched
			ret = LinkType.INTERRUPTED;
			break;
		case 19: // ehci_hcd:usb, well, at least on my machine
		case 23:
			ret = LinkType.USER_INPUT;
			break;
		default:
			ret = LinkType.UNKNOWN;
			break;
		}
		return ret;
	}


	private LinkType resolveSoftirq(CtfTmfEvent event) {
		int vec = (int) EventField.getLong(event, "vec");
		LinkType ret = LinkType.UNKNOWN;
		switch(vec) {
		case Softirq.HRTIMER:
		case Softirq.TIMER:
			ret = LinkType.TIMER;
			break;
		case Softirq.BLOCK:
		case Softirq.BLOCK_IOPOLL:
			ret = LinkType.BLOCK_DEVICE;
			break;
		case Softirq.NET_RX:
		case Softirq.NET_TX:
			ret = LinkType.NETWORK;
			break;
		case Softirq.SCHED:
			ret = LinkType.INTERRUPTED;
			break;
		default:
			ret = LinkType.UNKNOWN;
			break;
		}
		return ret;
	}

	public void debugMySockInet(CtfTmfEvent event) {
		Stack<InterruptContext> stack = system.getInterruptContext(event.getCPU());
		Context context = stack.peek().getContext();
		if (context == Context.NONE) {
			log.debug(event.getEventName() + "  " + system.getTaskCpu(event.getCPU()));
		} else {
			log.debug(event.getEventName() + "  " + context);
		}
	}

	public void handle_inet_sock_local_in(TraceReader reader, CtfTmfEvent event) {
		//debugMySockInet(event);
		Stack<InterruptContext> stack = system.getInterruptContext(event.getCPU());
		Context context = stack.peek().getContext();
		if (context == Context.SOFTIRQ) {
			Task k = kernel[event.getCPU()];
			Node endpoint = stateExtend(k, event.getTimestamp().getValue());
			tcpNodes.put(event,  endpoint);
			tcpMatching.matchEvent(event, 0);
		}
	}

	public void handle_inet_sock_local_out(TraceReader reader, CtfTmfEvent event) {
		//debugMySockInet(event);
		Stack<InterruptContext> stack = system.getInterruptContext(event.getCPU());
		Context context = stack.peek().getContext();
		Task sender = null;
		if (context == Context.NONE) {
			sender = system.getTaskCpu(event.getCPU());
		} else if (context == Context.SOFTIRQ) {
			sender = kernel[event.getCPU()];
		}
		if (sender == null)
			return;
		Node endpoint = stateExtend(sender, event.getTimestamp().getValue());
		tcpNodes.put(event,  endpoint);
		tcpMatching.matchEvent(event, 0);
	}

	public void handle_softirq_entry(TraceReader reader, CtfTmfEvent event) {
		int vec = (int) EventField.getLong(event, "vec");
		if (vec == Softirq.NET_RX || vec == Softirq.NET_TX) {
			Task k = kernel[event.getCPU()];
			graph.add(k, new Node(event.getTimestamp().getValue()));
		}
	}

	@Override
	public void handleComplete(TraceReader reader) {
		//log.message("init TraceEventHandlerExecutionGraph");
	}

}
