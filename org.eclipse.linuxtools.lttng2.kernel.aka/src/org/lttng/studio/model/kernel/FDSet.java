package org.lttng.studio.model.kernel;

import java.util.Collection;
import java.util.HashMap;

public class FDSet {

	private final HashMap<Long, FD> fdSet;

	public FDSet () {
		fdSet = new HashMap<Long, FD>();
	}

	public void addFD(FD fd) {
		if (fd == null)
			return;
		fdSet.put(fd.getNum(), fd);
	}

	public FD getFD(long num) {
		return fdSet.get(num);
	}

	public Collection<? extends FD> getFDs() {
		return fdSet.values();
	}

	public FD remove(FD fd) {
		return fdSet.remove(fd);
	}

}
