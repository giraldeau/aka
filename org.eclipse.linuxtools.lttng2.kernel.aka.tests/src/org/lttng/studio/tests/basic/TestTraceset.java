package org.lttng.studio.tests.basic;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

public class TestTraceset {

	private static String tracesetNotFound = "Traceset directory not found: Download the latest traceset with download-traceset.sh";
	
	public static File getLatestTraceset() throws IOException {
		File base = new File ("traceset");
		if (!base.isDirectory()) {
			throw new IOException(tracesetNotFound);
		}
		File[] files = base.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if (name.startsWith("lttng-traceset-"))
					return true;
				return false;
			}
		});
		if (files.length == 0) {
			throw new IOException(tracesetNotFound);
		}
		Arrays.sort(files);
		return files[files.length-1];
	}
	
	public static File getKernelTrace(String name) throws IOException {
		return findTraceDir(name, "kernel");
	}
	
	// FIXME: returns all UST sub-directories
	public static File getUSTTrace(String name) throws IOException {
		File traceDir = findTraceDir(name, "ust");
		File[] dirs = traceDir.listFiles();
		File found = null;
		for (File file: dirs) {
			if (file.isDirectory()) {
				found = file;
				break;
			}
		}
		return found;
	}
	
	public static File findTraceDir(String name, String type) throws IOException {
		File tracesetDir = getLatestTraceset();
		File traceDir = new File(tracesetDir, name);
		traceDir = new File(traceDir, type);
		if (!traceDir.isDirectory()) {
			throw new IOException("Trace " + traceDir.getName() + " doesn't exists in " + tracesetDir);
		}
		return traceDir;
	}
	
}
