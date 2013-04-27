package org.lttng.studio.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
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
import org.lttng.studio.reader.handler.ALog;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;

public class MainCriticalPath {

	static final String OP_LIST = "list";
	static final String OP_ANALYZE = "analyze";
	static final String OP_DEFAULT = OP_ANALYZE;
	static final String[] availOps = { OP_LIST, OP_ANALYZE };

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
		String msg = "processing";
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
			System.out.print("\r\n");
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
		opts.ctfTmfTrace = new CtfTmfTrace();
		try {
			opts.ctfTmfTrace.initTrace(null, opts.traceDir.getCanonicalPath(), CtfTmfEvent.class);
		} catch (TmfTraceException e) {
			System.err.println("Error loading trace " + opts.traceDir);
			e.printStackTrace();
			return;
		} catch (IOException e) {
			System.err.println("Error loading trace " + opts.traceDir);
			e.printStackTrace();
			return;
		}

		UUID uuid = opts.ctfTmfTrace.getCTFTrace().getUUID();
		System.out.println("Processing trace " + uuid.toString());
		if (opts.op.compareTo(OP_LIST) == 0) {
			self.listTasks(opts);
		} else if (opts.op.compareTo(OP_ANALYZE) == 0) {
			self.analyze(opts);
		} else {
			System.err.println("unknown operation " + opts.op);
			self.usage();
		}
		System.exit(0);
	}

	public void analyze(Opts opts) {
		AnalyzerThread thread = processTrace(opts);
		if (thread == null)
			return;
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
				System.err.println("unknown task " + tid);
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
				e.printStackTrace();
			}
		}
		Dot.writeString(uuid.toString(), "overall_graph.dot", Dot.todot(graph, list));
		spinner.done();
	}

	public void listTasks(Opts opts) {
		AnalyzerThread thread = processTrace(opts);
		if (thread == null)
			return;
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
		} catch (TmfTraceException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
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
			e.printStackTrace();
			return null;
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
		if (opts.traceDir == null) {
			throw new ParseException("trace path is required");
		}
		return opts;
	}

	private void usage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(this.getClass().getSimpleName(), "Available commands: [ list | analyze ]", options, "", true);
		System.exit(1);
	}

}
