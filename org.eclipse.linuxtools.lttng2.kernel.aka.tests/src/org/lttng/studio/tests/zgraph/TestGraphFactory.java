package org.lttng.studio.tests.zgraph;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.lttng.studio.model.zgraph.Dot;
import org.lttng.studio.model.zgraph.Graph;
import org.lttng.studio.model.zgraph.Node;

public class TestGraphFactory {

	public static final String A = "A";
	public static final String B = "B";
	public static final String C = "C";
	public static final String D = "D";
	public static final String E = "E";

	public static String GRAPH_TASK1 		= "task1";

	public static List<String> kind = new ArrayList<String>();
	static {
		kind.add(GRAPH_TASK1);
	}

	public static Graph make_task1() {
		Graph g = new Graph();
		for (int i = 0; i < 10; i++)
			g.append(A, new Node(i));
		return g;
	}

	public static Graph makeGraphByName(String name) {
		Method method = null;
		try {
			method = TestGraphFactory.class.getDeclaredMethod("make_" + name);
		} catch (NoSuchMethodException e1) {
			e1.printStackTrace();
		} catch (SecurityException e1) {
			e1.printStackTrace();
		}
		if (method == null)
			return null;
		try {
			return (Graph) method.invoke(null);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) throws IOException {
		File base = new File("results", TestGraphFactory.class.getName());
		base.mkdirs();
		TestGraphFactory factory = new TestGraphFactory();
		for (String name: kind) {
			String content = Dot.todot(makeGraphByName(name));
			TestGraph.writeString(factory, name + ".dot", content);
		}
	}

}
