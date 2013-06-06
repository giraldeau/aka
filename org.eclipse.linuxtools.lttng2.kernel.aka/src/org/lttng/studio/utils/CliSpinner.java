package org.lttng.studio.utils;

public class CliSpinner extends Thread {

	boolean done = false;
	static final String[] items = { "-", "\\", "|", "/" };
	String msg = "Writing results... ";

	@Override
	public void run() {
		int i = 0;
		while(!done) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.print("\r" + msg + " " + items[i]);
			i = (i + 1) % items.length;
		}
		System.out.print("\r" + msg + " done\n");
	}
	public void done() {
		done = true;
	}
}
