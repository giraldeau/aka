package org.lttng.studio.reader.handler;

import java.util.Set;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
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
	private ALog log;

	public WakeupContextHandler() {
		super();
		hooks.add(new TraceHook("sched_wakeup"));
		hooks.add(new TraceHook("softirq_entry"));
		hooks.add(new TraceHook("softirq_exit"));
		hooks.add(new TraceHook("irq_handler_entry"));
		hooks.add(new TraceHook("irq_handler_exit"));
	}

	public void handle_softirq_entry(TraceReader reader, CtfTmfEvent event) {
		context[event.getCPU()] = Context.SOFTIRQ;
	}

	public void handle_softirq_exit(TraceReader reader, CtfTmfEvent event) {
		context[event.getCPU()] = Context.DEFAULT;
	}

	public void handle_irq_handler_entry(TraceReader reader, CtfTmfEvent event) {
		context[event.getCPU()] = Context.IRQ;
	}

	public void handle_irq_handler_exit(TraceReader reader, CtfTmfEvent event) {
		context[event.getCPU()] = Context.DEFAULT;
	}

	public void handle_sched_wakeup(TraceReader reader, CtfTmfEvent event) {
		long wakeeTid = EventField.getLong(event, "tid");
		long curr = system.getCurrentTid(event.getCPU());
		Task currTask = system.getTask(curr);
		Task wakeeTask = system.getTask(wakeeTid);
		log.message(String.format("wakeup %10s %10s %10s", currTask, wakeeTask, context[event.getCPU()]));
	}

	@Override
	public void handleInit(TraceReader reader) {
		filter = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, AnalysisFilter.class);
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
		context = new Context[reader.getNumCpus()];
		log = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, ALog.class);
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}


}
