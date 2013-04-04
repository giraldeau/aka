package org.lttng.studio.utils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.lttng.studio.model.kernel.EventCounter;
import org.lttng.studio.reader.AnalysisPhase;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.TimeLoadingListener;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;

public class MainCriticalPath {

	static final String OP_LIST = "list";
	static final String OP_ANALYZE = "analyze";
	static final String OP_DEFAULT = OP_ANALYZE;
	static final String[] availOps = { OP_LIST, OP_ANALYZE };

	static Options options;

	public class Opts {
		public File traceDir;
		public Long pid;
		public String comm;
		public String op;
		CtfTmfTrace ctfTmfTrace;
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
		System.out.println(opts);

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

		if (opts.op.compareTo(OP_LIST) == 0) {
			self.listTasks(opts);
		} else if (opts.op.compareTo(OP_ANALYZE) == 0) {
			self.analyze(opts);
		} else {
			System.err.println("unknown operation " + opts.op);
			self.usage();
		}
		System.out.println("done");
	}

	public void analyze(Opts opts) {
	}

	public void listTasks(Opts opts) {
		AnalyzerThread thread = new AnalyzerThread();
		Collection<AnalysisPhase> phases = TraceEventHandlerFactory.makeStandardAnalysis();
		try {
			thread.setTrace(opts.traceDir);
		} catch (TmfTraceException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		thread.addAllPhases(phases);
		thread.setListener(new TimeLoadingListener("loading", phases.size(), new NullProgressMonitor()));
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		System.out.println(thread.getReader().getRegistry().getModel(IModelKeys.SHARED, EventCounter.class).getCounter());
	}

	private Opts processArgs(String[] args) throws ParseException {
		Opts opts = this.new Opts();
		options = new Options();
		options.addOption("t", true, "trace path");
		options.addOption("l", false, "list tasks");
		options.addOption("p", true, "pid to analyze");

		CommandLineParser parser = new PosixParser();
		CommandLine cmd = null;

		cmd = parser.parse(options, args);
		opts.op = OP_DEFAULT;
		String[] ops = cmd.getArgs();
		for (String op: ops) {
			for (String availOp: availOps) {
				if (op.compareTo(availOp) == 0)
					opts.op = op;
			}
		}

		if (cmd.hasOption("t")) {
			String trace = cmd.getOptionValue("t");
			opts.traceDir = new File(trace);
		}
		if (cmd.hasOption("p")) {
			opts.pid = new Long(cmd.getOptionValue("p"));
		}
		if (opts.traceDir == null) {
			throw new ParseException("trace path is required");
		}
		return opts;
	}

	private void usage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(this.getClass().getSimpleName(), options);
		System.exit(1);
	}

}
