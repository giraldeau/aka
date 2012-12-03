package org.lttng.studio.reader.handler;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDefinition;
import org.lttng.studio.fsa.Automaton;
import org.lttng.studio.fsa.AutomatonRecorder;
import org.lttng.studio.fsa.Transition;
import org.lttng.studio.fsa.pattern.BlockingAutomaton;
import org.lttng.studio.model.kernel.BlockingItem;
import org.lttng.studio.model.kernel.ModelEvent;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.kernel.WakeupInfo;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.TraceReader;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/*
 * Detect blocking in a process
 */
public class TraceEventHandlerBlocking extends TraceEventHandlerBase {
	public long count;
	HashMap<Task, AutomatonRecorder<EventDefinition>> automatMap;
	Multimap<Task, BlockingItem> blockings;
	HashMap<Task, WakeupInfo> taskWakeup;
	WakeupInfo[] wakeup;

	private SystemModel system;

	public TraceEventHandlerBlocking() {
		super();
		this.hooks.add(new TraceHook());
		this.hooks.add(new TraceHook("softirq_entry"));
		this.hooks.add(new TraceHook("softirq_exit"));
		this.hooks.add(new TraceHook("inet_sock_local_in"));
	}

	@Override
	public void handleInit(TraceReader reader) {
		system = reader.getRegistry().getOrCreateModel(IModelKeys.SHARED, SystemModel.class);
		system.init(reader);
		automatMap = new HashMap<Task, AutomatonRecorder<EventDefinition>>();
		wakeup = new WakeupInfo[reader.getNumCpus()];
		taskWakeup = new HashMap<Task, WakeupInfo>();
		blockings = ArrayListMultimap.create();
	}

	@Override
	public void handleComplete(TraceReader reader) {
	}

	public void handle_softirq_entry(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		long vec = ((IntegerDefinition) def.get("_vec")).getValue();
		WakeupInfo info = new WakeupInfo();
		info.vec = vec;
		wakeup[event.getCPU()] = info;
	}

	public void handle_softirq_exit(TraceReader reader, EventDefinition event) {
		wakeup[event.getCPU()] = null;
	}

	public void handle_inet_sock_local_in(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		long sk = ((IntegerDefinition) def.get("_sk")).getValue();
		long seq = ((IntegerDefinition) def.get("_seq")).getValue();
		WakeupInfo info = wakeup[event.getCPU()];
		if (info != null) {
			info.type = WakeupInfo.Type.SOCK;
			info.sk = sk;
			info.seq = seq;
		}
	}

	public void handle_all_event(TraceReader reader, EventDefinition event) {
		HashMap<String, Definition> def = event.getFields().getDefinitions();
		Task task = system.getTaskCpu(event.getCPU());
		AutomatonRecorder<EventDefinition> rec = getOrCreateAutomaton(task);
		String name = event.getDeclaration().getName();
		if (name.startsWith("sys_")) {
			rec.step("sys_entry", event);
		} else if (name.equals("exit_syscall")) {
			rec.step("sys_exit", event);
		} else if (name.equals("sched_switch")) {
			long prev = ((IntegerDefinition) def.get("_prev_tid")).getValue();
			long next = ((IntegerDefinition) def.get("_next_tid")).getValue();
			Task nextTask = system.getTask(next);
			Task prevTask = system.getTask(prev);
			AutomatonRecorder<EventDefinition> nextAut = getOrCreateAutomaton(nextTask);
			AutomatonRecorder<EventDefinition> prevAut = getOrCreateAutomaton(prevTask);
			prevAut.step("sched_out", event);
			nextAut.step("sched_in", event);
		} else if (name.equals("sched_wakeup")) {
			long tid = ((IntegerDefinition) def.get("_tid")).getValue();
			Task wakeTask = system.getTask(tid);
			AutomatonRecorder<EventDefinition> wakeAut = getOrCreateAutomaton(wakeTask);
			Transition step = wakeAut.step("wakeup", event);
			if (step == null)
				return;
			WakeupInfo info = wakeup[event.getCPU()];
			if (info != null)
				taskWakeup.put(wakeTask, info);
		}
		if (rec.getAutomaton().getState().isAccepts()) {
			ArrayList<EventDefinition> history = rec.getHistory();
			EventDefinition entry = history.get(0);
			EventDefinition exit = history.get(history.size()-1);
			BlockingItem blocking = new BlockingItem();
			//blocking.setInterval(new Interval(entry.getTimestamp(), exit.getTimestamp()));
			blocking.setSyscall(entry);
			blocking.setTask(task);
			if (taskWakeup.containsKey(task)) {
				WakeupInfo info = taskWakeup.get(task);
				blocking.setWakeupInfo(info);
			}
			ModelEvent ev = new ModelEvent();
			ev.blocking = blocking;
			ev.type = ModelEvent.BLOCKING;
			notifyListeners(ev.type, ev);
			blockings.put(task, blocking);
			rec.reset();
		}
	}

	private AutomatonRecorder<EventDefinition> getOrCreateAutomaton(Task task) {
		AutomatonRecorder<EventDefinition> rec = automatMap.get(task);
		if (rec == null) {
			Automaton aut = BlockingAutomaton.getInstance();
			rec = new AutomatonRecorder<EventDefinition>(aut);
			automatMap.put(task, rec);
		}
		return rec;
	}

}