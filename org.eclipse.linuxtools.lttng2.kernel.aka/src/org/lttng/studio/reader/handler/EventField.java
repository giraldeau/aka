package org.lttng.studio.reader.handler;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;

public class EventField {

	public static long getLong(ITmfEvent event, String name) {
		return (Long) event.getContent().getField(name).getValue();
	}

	public static String getString(ITmfEvent event, String name) {
		return (String) event.getContent().getField(name).getValue();
	}

	public static double getFloat(ITmfEvent event, String name) {
		return (Double) event.getContent().getField(name).getValue();
	}
}
