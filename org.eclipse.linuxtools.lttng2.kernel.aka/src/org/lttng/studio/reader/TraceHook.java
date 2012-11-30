package org.lttng.studio.reader;

import java.lang.reflect.Method;

import org.lttng.studio.reader.handler.ITraceEventHandler;

public class TraceHook implements Comparable<TraceHook> {
	
	public String eventName;
	public ITraceEventHandler instance;
	public Method method;
	
	public TraceHook(String eventName) {
		this.eventName = eventName; 
	}
	
	public TraceHook() {
		this(null);
	}
	
	public boolean isAllEvent() {
		return this.eventName == null;
	}

	public Integer getPriority() {
		if (instance == null)
			return 0;
		return instance.getPriority();
	}

	@Override
	public int compareTo(TraceHook other) {
		return this.getPriority().compareTo(other.getPriority());
	}
	
	@Override
	public String toString() {
		return this.eventName + ":" + getPriority();
	}
	
}
