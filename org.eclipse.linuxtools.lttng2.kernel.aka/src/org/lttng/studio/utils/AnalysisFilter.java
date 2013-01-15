package org.lttng.studio.utils;

import java.util.HashSet;
import java.util.Set;

import org.lttng.studio.model.kernel.ITraceModel;
import org.lttng.studio.reader.TraceReader;

public class AnalysisFilter  implements ITraceModel {

	HashSet<Long> tids;
	HashSet<String> commands;
	private boolean followChild;

	public AnalysisFilter() {
		tids = new HashSet<Long>();
		commands = new HashSet<String>();
	}

	@Override
	public void reset() {
	}

	@Override
	public void init(TraceReader reader) {
	}

	public void addTid(Long tid) {
		if (tid != null)
			tids.add(tid);
	}

	public Set<Long> getTids() {
		return tids;
	}

	public void setFollowChild(boolean followChild) {
		this.followChild = followChild;
	}

	public boolean isFollowChild() {
		return this.followChild;
	}

	public void addCommand(String comm) {
		if (comm != null)
			commands.add(comm);
	}

	public Set<String> getCommands() {
		return commands;
	}

}
