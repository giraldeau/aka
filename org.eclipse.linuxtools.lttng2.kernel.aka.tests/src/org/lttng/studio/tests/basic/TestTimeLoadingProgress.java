package org.lttng.studio.tests.basic;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Test;
import org.lttng.studio.reader.TimeLoadingListener;
import org.lttng.studio.reader.TraceReader;

public class TestTimeLoadingProgress {

	int max = 0;
	ArrayList<Integer> res;
	@Test
	public void testTimeListener() throws Exception {
		res = new ArrayList<Integer>();
		TimeLoadingListener listener = new TimeLoadingListener("default", new IProgressMonitor() {

			@Override
			public void beginTask(String name, int totalWork) {
				max = totalWork;
			}

			@Override
			public void done() {
				// TODO Auto-generated method stub

			}

			@Override
			public void internalWorked(double work) {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean isCanceled() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void setCanceled(boolean value) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setTaskName(String name) {
				// TODO Auto-generated method stub

			}

			@Override
			public void subTask(String name) {
				// TODO Auto-generated method stub

			}

			@Override
			public void worked(int work) {
				res.add(work);
			}

		});

		File trace = TestTraceset.getKernelTrace("netcat-tcp-k");
		TraceReader reader = new TraceReader();
		reader.addTrace(trace);
		reader.process(listener);
		//System.out.println(res);
		assertTrue(res.size() > 100);
		assertTrue(res.size() < max);
	}

}
