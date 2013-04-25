package org.lttng.studio.reader;

import org.eclipse.core.runtime.IProgressMonitor;

public class CliProgressMonitor implements IProgressMonitor {

	/* Thanks to:
	 * http://nakkaya.com/2009/11/08/command-line-progress-bar/
	 */
	public static void printProgBar(int percent) {
		StringBuilder bar = new StringBuilder("[");

		for (int i = 0; i < 50; i++) {
			if (i < (percent / 2)) {
				bar.append("=");
			} else if (i == (percent / 2)) {
				bar.append(">");
			} else {
				bar.append(" ");
			}
		}

		bar.append("]   " + percent + "%     ");
		System.out.print("\r" + bar.toString());
	}

	private int totalWork;

	@Override
	public void beginTask(String name, int totalWork) {
		totalWork--;
		// avoid division by zero
		if (totalWork == 0)
			totalWork = 1;
		this.totalWork = totalWork;
	}

	@Override
	public void done() {
	}

	@Override
	public void internalWorked(double work) {
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void setCanceled(boolean value) {
	}

	@Override
	public void setTaskName(String name) {
	}

	@Override
	public void subTask(String name) {
	}

	@Override
	public void worked(int work) {
		int progress = 100 * work / totalWork;
		printProgBar(progress);
	}

}
