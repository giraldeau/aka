package org.lttng.studio.model.kernel;

import java.util.Comparator;

public class TaskStartComparator implements Comparator<Task> {

	@Override
	public int compare(Task t1, Task t2) {
		if (t1.getStart() > t2.getStart())
			return 1;
		if (t1.getStart() < t2.getStart())
			return -1;
		return 0;
	}

}
