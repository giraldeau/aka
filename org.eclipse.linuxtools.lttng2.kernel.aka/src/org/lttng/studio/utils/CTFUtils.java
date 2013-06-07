package org.lttng.studio.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import org.eclipse.linuxtools.ctf.core.trace.CTFTrace;
import org.eclipse.linuxtools.ctf.core.trace.CTFTraceReader;
import org.eclipse.linuxtools.ctf.core.trace.StreamInputReader;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.lttng.studio.reader.Host;

public class CTFUtils {

	public static final String UUID_FIELD = "uuid";
	public static final String HOST_FIELD = "hostname";

	public static String unquote(String s) {
		if (s != null && (
				s.startsWith("\"") && s.endsWith("\"") ||
				s.startsWith("\'") && s.endsWith("\'"))) {
			s = s.substring(1, s.length() - 1);
		}
		return s;
	}

	public static void setTraceClockUUID(CTFTrace trace, UUID uuid) {
		trace.getClock().addAttribute(UUID_FIELD, "\"" + uuid + "\"");
	}

	public static String getTraceClockUUIDStringRaw(CTFTrace trace) {
		return (String) trace.getClock().getProperty(UUID_FIELD);
	}
	public static String getTraceClockUUIDString(CTFTrace trace) {
		return unquote(getTraceClockUUIDStringRaw(trace));
	}

	public static UUID getTraceClockUUID(CTFTrace trace) {
		return UUID.fromString(getTraceClockUUIDString(trace));
	}

	public static String getTraceHostnameRaw(CTFTrace trace) {
		return trace.getEnvironment().get(HOST_FIELD);
	}

	public static String getTraceHostname(CTFTrace trace) {
		return unquote(getTraceHostnameRaw(trace));
	}

	public static List<CtfTmfTrace> getCtfTraceList(ITmfTrace trace) {
		ArrayList<CtfTmfTrace> traces = new ArrayList<CtfTmfTrace>();
		if (trace instanceof CtfTmfTrace) {
			traces.add((CtfTmfTrace) trace);
		} else if (trace instanceof TmfExperiment) {
			TmfExperiment exp = (TmfExperiment) trace;
			for (ITmfTrace t: exp.getTraces()) {
				if (t instanceof CtfTmfTrace) {
					traces.add((CtfTmfTrace) t);
				}
			}
		}
		return traces;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int getNumCpuFromCtfTrace(CtfTmfTrace ctf) {
		int cpus = 0;
		CTFTraceReader reader = new CTFTraceReader(ctf.getCTFTrace());
		Field field;
		Vector<StreamInputReader> v;
		try {
			field = reader.getClass().getDeclaredField("streamInputReaders");
			field.setAccessible(true);
			v = (Vector) field.get(reader);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error trying to retreive the number of CPUs of the trace");
		}
		for (StreamInputReader input: v) {
			int cpu = input.getCPU();
			cpus = Math.max(cpus, cpu + 1);
		}
		cpus = Math.max(cpus, v.size());
		return cpus;
	}

	public static CtfTmfTrace makeCtfTrace(File file) throws TmfTraceException, IOException {
		CtfTmfTrace ctfTrace = new CtfTmfTrace();
		ctfTrace.initTrace(null, file.getCanonicalPath(), ITmfEvent.class);
		return ctfTrace;
	}

	public static TmfExperiment makeTmfExperiment(ITmfTrace[] traceSet) {
		return new TmfExperiment(ITmfEvent.class, "none", traceSet);
	}

	public static TmfExperiment makeTmfExperiment(File[] files) throws TmfTraceException, IOException {
		CtfTmfTrace[] ctf = new CtfTmfTrace[files.length];
		for (int i = 0; i < files.length; i++) {
			ctf[i] = new CtfTmfTrace();
			ctf[i].initTrace(null, files[i].getCanonicalPath(), ITmfEvent.class);
		}
		return makeTmfExperiment(ctf);
	}

	public static Host hostFromTrace(CtfTmfTrace ctfTrace) {
		CTFTrace ctf = ctfTrace.getCTFTrace();
		UUID uuid = getTraceClockUUID(ctf);
		String hostname = getTraceHostname(ctf);
		int cpus = CTFUtils.getNumCpuFromCtfTrace(ctfTrace);
		return new Host(uuid, hostname, cpus);
	}

}
