package org.lttng.studio.reader.handler;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.utils.AnalysisFilter;

import com.google.common.collect.HashBasedTable;

public class EventContextHandler extends TraceEventHandlerBase {

	private static String[] contextNames = { "CONTEXT_TASK", "CONTEXT_SOFTIRQ", "CONTEXT_IRQ", "CONTEXT_HRTIMER" };
	
	private static int CONTEXT_TASK = 0;
	private static int CONTEXT_SOFTIRQ = 1;
	private static int CONTEXT_IRQ = 2;
	private static int CONTEXT_HRTIMER = 3;
	private static int CONTEXT_MAX = 4;
	
	private SystemModel system;
	private CtfTmfEvent[] context;
	private AnalysisFilter filter;
	private HashSet<String> eventSet;
	private ALog log;
	private HashMap<Long, Boolean> cache;
	private HashMap<Long, String> eventNames;
	private HashMap<Long, Long[]> contextStats;

	private int[] contextID;

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

	public void handle_softirq_entry(TraceReader reader, CtfTmfEvent event) {
		contextID[event.getCPU()] = CONTEXT_SOFTIRQ;
		context[event.getCPU()] = event;
	}

	public void handle_softirq_exit(TraceReader reader, CtfTmfEvent event) {
		contextID[event.getCPU()] = CONTEXT_TASK;
		context[event.getCPU()] = null;
	}

	public void handle_irq_handler_entry(TraceReader reader, CtfTmfEvent event) {
		contextID[event.getCPU()] = CONTEXT_IRQ;
		context[event.getCPU()] = event;
	}

	public void handle_irq_handler_exit(TraceReader reader, CtfTmfEvent event) {
		contextID[event.getCPU()] = CONTEXT_TASK;
		context[event.getCPU()] = null;
	}

	public void handle_hrtimer_expire_entry(TraceReader reader, CtfTmfEvent event) {
		contextID[event.getCPU()] = CONTEXT_HRTIMER;
		context[event.getCPU()] = event;
	}

	public void handle_hrtimer_expire_exit(TraceReader reader, CtfTmfEvent event) {
		contextID[event.getCPU()] = CONTEXT_TASK;
		context[event.getCPU()] = null;
	}
	
	public void handle_all_event(TraceReader reader, CtfTmfEvent event) {
		if (!cache.containsKey(event.getID())) {
			boolean enable = eventSet.contains(event.getEventName());
			cache.put(event.getID(), enable);
			eventNames.put(event.getID(), event.getEventName());
		}
		if (cache.get(event.getID())) {
			int ctx = contextID[event.getCPU()];
			if (!contextStats.containsKey(event.getID())) {
				Long[] table = new Long[CONTEXT_MAX];
				for (int i = 0; i < CONTEXT_MAX; i++) {
					table[i] = new Long(0);
				}
				contextStats.put(event.getID(), table);
			}
			Long[] stats = contextStats.get(event.getID());
			stats[ctx]++;
			if (contextID[event.getCPU()] == CONTEXT_TASK) {
				long curr = system.getCurrentTid(event.getCPU());
				Task currTask = system.getTask(curr);
				log.message(String.format("%s %s %s", contextNames[ctx],
					currTask, dumpEvent(event)));
			} else {
				log.message(String.format("%s %s %s %s",
					contextNames[ctx],
					context[event.getCPU()].getTimestamp(),
					context[event.getCPU()].getEventName(),
					dumpEvent(event)));
			}
		}
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
		filter = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, AnalysisFilter.class);
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
		contextStats = new HashMap<Long, Long[]>();
		contextID = new int[reader.getNumCpus()];
		context = new CtfTmfEvent[reader.getNumCpus()];
		log = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, ALog.class);
		cache = new HashMap<Long, Boolean>();
		eventNames = new HashMap<Long, String>();
	}

	@Override
	public void handleComplete(TraceReader reader) {
		StringBuilder str = new StringBuilder();
		str.append("Event;");
		for (String name: contextNames) {
			str.append(name + ";");
		}
		str.append("\n");
		for (Long id: contextStats.keySet()) {
			str.append(eventNames.get(id) + ";");
			for (Long count: contextStats.get(id)) {
				str.append(count + ";");
			}
			str.append("\n");
		}
		System.out.println(str.toString());
	}

	public void addEventName(String name) {
		eventSet.add(name);
	}

}
