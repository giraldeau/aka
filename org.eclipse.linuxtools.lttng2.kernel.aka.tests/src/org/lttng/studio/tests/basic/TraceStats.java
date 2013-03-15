package org.lttng.studio.tests.basic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.event.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.lttng.studio.model.kernel.EventCounter;
import org.lttng.studio.reader.AnalysisPhase;
import org.lttng.studio.reader.AnalyzerThread;
import org.lttng.studio.reader.handler.IModelKeys;
import org.lttng.studio.reader.handler.ITraceEventHandler;
import org.lttng.studio.reader.handler.TraceEventHandlerFactory;

public class TraceStats {

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		String[] kernelTraceset = TestTraceset.getKernelTraceset();
		for (String trace: kernelTraceset) {
			analyze(trace);
		}
	}

	private static void analyze(String trace) throws IOException, InterruptedException {
		File traceDir = TestTraceset.getKernelTrace(trace);
		System.out.println("traceDir: " + traceDir.getCanonicalPath());
		AnalyzerThread thread = new AnalyzerThread();
		CtfTmfTrace ctfTmfTrace = new CtfTmfTrace();
		try {
			ctfTmfTrace.initTrace(null, traceDir.getCanonicalPath(), CtfTmfEvent.class);
		} catch (TmfTraceException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		Collection<ITraceEventHandler> phase = TraceEventHandlerFactory.makeBasic();

		thread.setTrace(ctfTmfTrace);
		thread.addPhase(new AnalysisPhase(1, "phase1", phase));
		thread.start();
		System.out.println("processing...");
		thread.join();
		System.out.println("done");

		ITmfTimestamp startTime = ctfTmfTrace.getStartTime();
		ITmfTimestamp endTime = ctfTmfTrace.getEndTime();
		ITmfTimestamp delta = endTime.getDelta(startTime);

		EventCounter counter = thread.getReader().getRegistry().getModel(IModelKeys.SHARED, EventCounter.class);
		double rate = 1000000000.0 * counter.getCounter() / delta.getValue();

		String[] split = traceDir.getCanonicalPath().split("/");
		String traceName = split[split.length - 2];
		System.out.println("Trace duration (ns) " + delta.getValue());
		System.out.println("NbEvents            " + counter.getCounter());
		System.out.println(String.format("Avg event rate      %.3f", rate));
		File out = new File("stats.out");
		FileWriter writer = new FileWriter(out, true);
		writer.write(String.format("%s;%d;%d;%.3f;\n", traceName, delta.getValue(), counter.getCounter(), rate));
		writer.flush();
		writer.close();
	}

}
