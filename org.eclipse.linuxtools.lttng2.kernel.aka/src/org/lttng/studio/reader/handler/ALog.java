package org.lttng.studio.reader.handler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ALog {

	public final static int NONE = 0;
	public final static int ERROR = 1;
	public final static int WARNING = 2;
	public final static int MESSAGE = 3;
	public final static int VERBOSE = 4;
	public final static int DEBUG = 5;

	private final static String[] labels = new String[] {
		"NONE", "ERROR", "WARNING", "MESSAGE", "VERBOSE", "DEBUG"
	};

	private int level;
	private FileWriter log;
	private String path;
	private boolean isError;

	public ALog() {
		setLevel(NONE);
	}

	public void init() {
		if (log == null) {
			try {
				log = new FileWriter(new File(getPath()));
			} catch (IOException e) {
				e.printStackTrace();
				isError = true;
			}
		}
		message("ALog initialized");
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		if (level > DEBUG)
			level = DEBUG;
		if (level < NONE)
			level = NONE;
		this.level = level;
	}

	private void entry(int level, String msg) {
		if (level > DEBUG)
			level = DEBUG;
		if (level < NONE)
			level = NONE;
		if (level > this.level)
			return;
		if (log == null || isError)
			return;
		try {
			log.write(String.format("%d %s %s\n",
					System.currentTimeMillis(),
					labels[level], msg));
		} catch (IOException e) {
			e.printStackTrace();
			isError = true;
		}
	}

	public void error(String msg) {
		entry(ERROR, msg);
	}

	public void warning(String msg) {
		entry(WARNING, msg);
	}

	public void message(String msg) {
		entry(MESSAGE, msg);
	}

	public void verbose(String msg) {
		entry(VERBOSE, msg);
	}

	public void debug(String msg) {
		entry(DEBUG, msg);
	}

	public boolean isError() {
		return this.isError;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		log = null;
		this.path = path;
		init();
	}

}
