package org.lttng.studio.reader.handler;

import java.util.HashMap;
import java.util.Set;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDefinition;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;
import org.lttng.studio.utils.AnalysisFilter;


public class WakeupContextHandler extends TraceEventHandlerBase {

	private enum Context {
		DEFAULT,
		IRQ,
		SOFTIRQ
	}

	private SystemModel system;
	private Context[] context;
	private AnalysisFilter filter;

	public WakeupContextHandler() {
		super();
		hooks.add(new TraceHook("sched_wakeup"));
		hooks.add(new TraceHook("softirq_entry"));
		hooks.add(new TraceHook("softirq_exit"));
		hooks.add(new TraceHook("irq_handler_entry"));
		hooks.add(new TraceHook("irq_handler_exit"));
	}

	public void handle_softirq_entry(TraceReader reader, EventDefinition event) {
		context[event.getCPU()] = Context.SOFTIRQ;
	}

	public void handle_softirq_exit(TraceReader reader, EventDefinition event) {
		context[event.getCPU()] = Context.DEFAULT;
	}

	public void handle_irq_handler_entry(TraceReader reader, EventDefinition event) {
		context[event.getCPU()] = Context.IRQ;
	}

	public void handle_irq_handler_exit(TraceReader reader, EventDefinition event) {
		context[event.getCPU()] = Context.DEFAULT;
	}

	public void handle_sched_wakeup(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		IntegerDefinition wakee = (IntegerDefinition) def.get("_tid");
		long curr = system.getCurrentTid(event.getCPU());
		Task currTask = system.getTask(curr);
		Task wakeeTask = system.getTask(wakee.getValue());
		Set<Long> tids = filter.getTids();
		if (tids.contains(curr) || tids.contains(wakee.getValue())) {
			System.out.println(String.format("wakeup %10s %10s %10s", currTask, wakeeTask, context[event.getCPU()]));
		}
	}

	@Override
	public void handleInit(TraceReader reader) {
		filter = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, AnalysisFilter.class);
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
		context = new Context[reader.getNumCpus()];
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}


}
