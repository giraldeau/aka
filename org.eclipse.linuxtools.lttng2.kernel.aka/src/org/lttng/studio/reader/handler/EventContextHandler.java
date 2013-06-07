package org.lttng.studio.reader.handler;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.lttng.studio.model.kernel.InterruptContext;
import org.lttng.studio.model.kernel.InterruptContext.Context;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

public class EventContextHandler extends TraceEventHandlerBase {

	private final HashSet<String> eventSet;
	private HashMap<String, EnumMap<Context, Long>> contextStats;

	public EventContextHandler() {
		super();
		hooks.add(new TraceHook());
		hooks.add(new TraceHook("softirq_entry"));
		hooks.add(new TraceHook("softirq_exit"));
		hooks.add(new TraceHook("hrtimer_expire_entry"));
		hooks.add(new TraceHook("hrtimer_expire_exit"));
		hooks.add(new TraceHook("irq_handler_entry"));
		hooks.add(new TraceHook("irq_handler_exit"));
		eventSet = new HashSet<String>();
	}

	private void pushInterruptContext(TraceReader reader, CtfTmfEvent event, Context ctx) {
		SystemModel system = reader.getRegistry().getModelForTrace(event.getTrace(), SystemModel.class);
		Stack<InterruptContext> stack = system.getInterruptContext(event.getCPU());
		stack.push(new InterruptContext(event, ctx));
	}

	private void popInterruptContext(TraceReader reader, CtfTmfEvent event, Context ctx) {
		SystemModel system = reader.getRegistry().getModelForTrace(event.getTrace(), SystemModel.class);
		Stack<InterruptContext> stack = system.getInterruptContext(event.getCPU());
		if (stack.isEmpty()) {
			//log.warning("popInterruptContext stack is empty " + event.toString());
			return;
		}
		if (stack.peek().getContext() == ctx) {
			stack.pop();
		} else {
			//log.warning("popInterruptContext unexpected top stack context " + event);
		}
	}

	public void handle_softirq_entry(TraceReader reader, CtfTmfEvent event) {
		pushInterruptContext(reader, event, Context.SOFTIRQ);
	}

	public void handle_softirq_exit(TraceReader reader, CtfTmfEvent event) {
		popInterruptContext(reader, event, Context.SOFTIRQ);
	}

	public void handle_irq_handler_entry(TraceReader reader, CtfTmfEvent event) {
		pushInterruptContext(reader, event, Context.IRQ);
	}

	public void handle_irq_handler_exit(TraceReader reader, CtfTmfEvent event) {
		popInterruptContext(reader, event, Context.IRQ);
	}

	public void handle_hrtimer_expire_entry(TraceReader reader, CtfTmfEvent event) {
		pushInterruptContext(reader, event, Context.HRTIMER);
	}

	public void handle_hrtimer_expire_exit(TraceReader reader, CtfTmfEvent event) {
		popInterruptContext(reader, event, Context.HRTIMER);
	}

	public void handle_all_event(TraceReader reader, CtfTmfEvent event) {
		if (!eventSet.contains(event.getEventName())) {
			return;
		}
		SystemModel system = reader.getRegistry().getModelForTrace(event.getTrace(), SystemModel.class);
		Stack<InterruptContext> ctxStack = system.getInterruptContext(event.getCPU());
		EnumMap<Context, Long> stats = contextStats.get(event.getEventName());
		Context ctx = ctxStack.peek().getContext();
		Long count = stats.get(ctx);
		count++;
		stats.put(ctx, count);
//		if (ctx == Context.NONE) {
//			long curr = system.getCurrentTid(event.getCPU());
//			Task currTask = system.getTask(curr);
//			log.message(String.format("%s %s %s", ctx,
//				currTask, dumpEvent(event)));
//		} else {
//			log.message(String.format("%s %s",
//				ctx,
//				dumpEvent(event)));
//		}
	}

	private static String dumpEvent(CtfTmfEvent event) {
		StringBuilder str = new StringBuilder();
		str.append("[");
		str.append(event.getTimestamp());
		str.append("] ");
		str.append(event.getEventName());
		str.append(" { ");
		for (ITmfEventField field: event.getContent().getFields()) {
			str.append(field.getName());
			str.append("=");
			str.append(field.getFormattedValue());
			str.append(" ");
		}
		str.append("}");
		return str.toString();
	}

	@Override
	public void handleInit(TraceReader reader) {
		reader.getRegistry().register(IModelKeys.PER_HOST, SystemModel.class);
		for (CtfTmfTrace ctf: reader.getTraces()) {
			SystemModel system = reader.getRegistry().getModelForTrace(ctf, SystemModel.class);
			system.init(reader, ctf);
		}
		// init statistics
		contextStats = new HashMap<String, EnumMap<Context,Long>>();
		for (String name: eventSet) {
			EnumMap<Context, Long> map = new EnumMap<Context, Long>(Context.class);
			for (Context ctx: Context.values()) {
				map.put(ctx, new Long(0));
			}
			contextStats.put(name, map);
		}

	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

	public void addEventName(String name) {
		eventSet.add(name);
	}

}
