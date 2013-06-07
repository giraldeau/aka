package org.lttng.studio.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*
 * Tests the cost of dynamic invoke compared to direct method call
 */

public class MethodBench {

	public static class Dummy {
		private int n;
		public Dummy() { n = 0; }
		public void foo() { n++; }
		public int n() { return n; }
	}

	public static abstract class MethodBenchmark {
		Dummy dummy;
		public MethodBenchmark() { dummy = new Dummy(); }
		public Dummy getDummy() { return dummy; }
		public void call() { }
	}

	public static class StaticMethodBenchmark extends MethodBenchmark {
		public StaticMethodBenchmark() {
			super();
		}
		@Override
		public void call() {
			dummy.foo();
		}
	}

	public static class DynamicMethodBenchmark extends MethodBenchmark {
		private Method method;
		public DynamicMethodBenchmark() {
			super();
			try {
				method = dummy.getClass().getMethod("foo", (Class<?>[]) null);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
		@Override
		public void call() {
			try {
				method.invoke(dummy);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	public static class BenchmarkRunner {
		private final long iter;
		public BenchmarkRunner(long iter) {
			this.iter = iter;
		}
		public long bench(MethodBenchmark b) throws InterruptedException {
			long t1 = System.currentTimeMillis();
			for (long i = 0; i < iter; i++) {
				b.call();
			}
			long t2 = System.currentTimeMillis();
			return t2 - t1;
		}
	}

	public static void main(String[] args) throws InterruptedException {
		long iter = 10000000000L;
		BenchmarkRunner runner = new BenchmarkRunner(iter);
		MethodBenchmark[] tests = new MethodBenchmark[] { new StaticMethodBenchmark(), new DynamicMethodBenchmark() };
		for (MethodBenchmark test: tests) {
			System.out.print(String.format("%-25s %f\n", test.getClass().getSimpleName(), runner.bench(test) / (double) 1000));
		}
	}

}
