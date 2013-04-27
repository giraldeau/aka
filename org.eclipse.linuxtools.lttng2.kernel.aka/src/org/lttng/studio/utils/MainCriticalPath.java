package org.lttng.studio.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.lttng.studio.model.kernel.SystemModel;
import org.lttng.studio.model.kernel.Task;
import org.lttng.studio.model.zgraph.CriticalPath;
import org.lttng.studio.model.zgraph.Dot;
import org.lttng.studio.model.zgraph.Graph;
import org.lttng.studio.model.zgraph.GraphStats;
import org.lttng.studio.model.zgraph.Ops;
import org.lttng.studio.reader.AnalysisPhase;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.CliProgressMonitor;
import org.lttng.studio.reader.TimeLoadingListener;
import org.lttng.studio.reader.TraceHook;
import org.lttng.studio.reader.handler.ALog;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.reader.handler.ITraceEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;

public class MainCriticalPath {

	static final String OP_LIST = "list";
	static final String OP_ANALYZE = "analyze";
	static final String OP_ENEV = "enable-event";
	static final String OP_DEFAULT = OP_ANALYZE;
	static final String[] availOps = { OP_LIST, OP_ANALYZE, OP_ENEV };

	static final String ALGO_BOUNDED = "bounded";
	static final String ALGO_UNBOUNDED = "unbounded";
	static final String ALGO_DEFAULT = ALGO_BOUNDED;
	static final String[] availAlgo = { ALGO_BOUNDED, ALGO_UNBOUNDED };

	static Options options;

	public class Opts {
		public File traceDir;
		public List<Long> tids = new ArrayList<Long>();
		public String comm;
		public String op;
		public String algo = ALGO_DEFAULT;
		CtfTmfTrace ctfTmfTrace;
	}

	public static class CliSpinner extends Thread {
		boolean done = false;
		static final String[] items = { "-", "\\", "|", "/" };
		String msg = "Writing results... ";
		@Override
		public void run() {
			int i = 0;
			while(!done) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.print("\r" + msg + " " + items[i]);
				i = (i + 1) % items.length;
			}
			System.out.print("\r" + msg + " done\n");
		}
		public void done() {
			done = true;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MainCriticalPath self = new MainCriticalPath();
		Opts opts;
		try {
			opts = self.processArgs(args);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			self.usage();
			return;
		}
		new File("results").mkdirs();
		if (opts.op.compareTo(OP_LIST) == 0) {
			self.listTasks(opts);
		} else if (opts.op.compareTo(OP_ANALYZE) == 0) {
			self.analyze(opts);
		} else if (opts.op.compareTo(OP_ENEV) == 0) {
			self.enableEvent(opts);
		} else {
			System.err.println("unknown operation " + opts.op);
			self.usage();
		}
		System.exit(0);
	}

	public void loadTrace(Opts opts) {
		if (opts.traceDir == null) {
			throw new RuntimeException("missing trace argument");
		}
		opts.ctfTmfTrace = new CtfTmfTrace();
		try {
			opts.ctfTmfTrace.initTrace(null, opts.traceDir.getCanonicalPath(), CtfTmfEvent.class);
		} catch (Exception e) {
			throw new RuntimeException("Error loading trace " + opts.traceDir);
		}
		UUID uuid = opts.ctfTmfTrace.getCTFTrace().getUUID();
		System.out.println("Processing trace " + uuid.toString());
	}

	public void enableEvent(Opts opts) {
		Collection<AnalysisPhase> phases = TraceEventHandlerFactory.makeStandardAnalysis();
		HashSet<String> evNames = new HashSet<String>();
		for (AnalysisPhase phase: phases) {
			Collection<ITraceEventHandler> handlers = phase.getHandlers();
			for (ITraceEventHandler handler: handlers) {
				for (TraceHook hook: handler.getHooks()) {
					if (hook.eventName != null) {
						// skip syscalls
						if (hook.eventName.matches("exit_syscall") ||
								hook.eventName.matches("sys_.*"))
							continue;
						evNames.add(hook.eventName);
					}
				}
			}
		}
		List<String> sorted = new ArrayList<String>(evNames);
		Collections.sort(sorted);
		for (String ev: sorted) {
			System.out.println(ev);
		}
	}

	public void analyze(Opts opts) {
		loadTrace(opts);
		AnalyzerThread thread = processTrace(opts);
		CliSpinner spinner = new CliSpinner();
		spinner.start();
		UUID uuid = opts.ctfTmfTrace.getCTFTrace().getUUID();
		SystemModel model = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
		Graph graph = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, Graph.class);
		Dot.setLabelProvider(Dot.pretty);
		CriticalPath cp = new CriticalPath(graph);
		Graph path = null;
		List<Task> list = new ArrayList<Task>();
		for (Long tid :opts.tids) {
			Task task = model.getTask(tid);
			if (task == null) {
				System.err.println("task not found: " + tid);
				continue;
			}
			list.add(task);
			List<Task> sub = new ArrayList<Task>();
			sub.add(task);
			Dot.writeString(uuid.toString(), tid + "_graph.dot", Dot.todot(graph, sub));
			try {
				path = cp.criticalPathBounded(graph.getHead(task));
				Dot.writeString(uuid.toString(), tid + "_path.dot", Dot.todot(path));

				GraphStats gstats = new GraphStats(path);
				Dot.writeString(uuid.toString(), tid + "_path.stats", gstats.dump());

				Ops.minimize(path.getHead(task));
				Dot.writeString(uuid.toString(), tid + "_path_min.dot", Dot.todot(path));

				GraphStats gstatsMin = new GraphStats(path);
				Dot.writeString(uuid.toString(), tid + "_path_min.stats", gstatsMin.dump());
			} catch (Exception e) {
				System.err.println("Error processing graph " + opts.traceDir);
			}
		}
		Dot.writeString(uuid.toString(), "overall_graph.dot", Dot.todot(graph, list));
		spinner.done();
		try {
			spinner.join();
		} catch (InterruptedException e) {
		}
	}

	public void listTasks(Opts opts) {
		loadTrace(opts);
		AnalyzerThread thread = processTrace(opts);
		String fmt = "%-5d %-5d %s\n";
		String fmtHeader = "%-5s %-5s %s\n";
		SystemModel model = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, SystemModel.class);
		Collection<Task> tasks = model.getTasks();
		System.out.print(String.format(fmtHeader, "tid", "pid", "cmd"));
		for (Task task: tasks) {
			System.out.print(String.format(fmt, task.getTid(), task.getPid(), task.getName()));
		}
	}

	private AnalyzerThread processTrace(Opts opts) {
		AnalyzerThread thread = new AnalyzerThread();
		Collection<AnalysisPhase> phases = TraceEventHandlerFactory.makeStandardAnalysis();
		try {
			thread.setTrace(opts.traceDir);
		} catch (Exception e) {
			throw new RuntimeException("Error while loading trace");
		}
		thread.addAllPhases(phases);
		thread.setListener(new TimeLoadingListener("loading", phases.size(), new CliProgressMonitor()));
		ALog log = thread.getReader().getRegistry().getOrCreateModel(IModelKeys.SHARED, ALog.class);
		log.setLevel(ALog.DEBUG);
		log.setPath("results/" + opts.ctfTmfTrace.getCTFTrace().getUUID() + ".log");
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted");
		}
		System.out.print("\n");
		return thread;
	}

	private Opts processArgs(String[] args) throws ParseException {
		Opts opts = this.new Opts();
		options = new Options();
		options.addOption("t", true, "trace path");
		options.addOption("a", true, "algorithm [bounded|unbounded]");
		options.addOption("p", true, "tid to analyze");

		CommandLineParser parser = new PosixParser();
		CommandLine cmd = null;

		cmd = parser.parse(options, args);
		String[] ops = cmd.getArgs();
		for (String op: ops) {
			for (String availOp: availOps) {
				if (op.compareTo(availOp) == 0)
					opts.op = op;
			}
		}
		if (opts.op == null) {
			throw new ParseException("unknown operation " + opts.op);
		}

		if (cmd.hasOption("a")) {
			opts.algo = cmd.getOptionValue("a");
			boolean found = false;
			for (String algo: availAlgo) {
				if (algo.compareTo(opts.algo) == 0) {
					found = true;
				}
			}
			if (!found) {
				throw new ParseException("unknown algorithm " + opts.algo);
			}
		}
		if (cmd.hasOption("t")) {
			String trace = cmd.getOptionValue("t");
			opts.traceDir = new File(trace);
		}
		if (cmd.hasOption("p")) {
			String tids = cmd.getOptionValue("p");
			String[] split = tids.split(",");
			for (String s: split)
				opts.tids.add(new Long(s));
		}
		return opts;
	}

	private void usage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(this.getClass().getSimpleName(), "Available commands: [ list | analyze ]", options, "", true);
		System.exit(1);
	}

}
