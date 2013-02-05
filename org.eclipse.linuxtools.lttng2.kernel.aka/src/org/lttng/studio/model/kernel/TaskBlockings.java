package org.lttng.studio.model.kernel;

import com.google.common.collect.ArrayListMultimap;

public class TaskBlockings {

	private final ArrayListMultimap<Task, TaskBlockingEntry> entries;

	public TaskBlockings() {
		entries = ArrayListMultimap.create();
	}

	public ArrayListMultimap<Task, TaskBlockingEntry> getEntries() {
		return entries;
	}

}
