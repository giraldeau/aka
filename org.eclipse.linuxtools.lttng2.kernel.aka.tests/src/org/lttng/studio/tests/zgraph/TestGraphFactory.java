package org.lttng.studio.tests.zgraph;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;

import org.lttng.studio.model.zgraph.Dot;
import org.lttng.studio.model.zgraph.Graph;
import org.lttng.studio.model.zgraph.GraphBuilder;
import org.lttng.studio.model.zgraph.Node;

public class TestGraphFactory {

	public static final String A = "A";
	public static final String B = "B";
	public static final String C = "C";
	public static final String D = "D";
	public static final String E = "E";

	private final HashMap<String, GraphBuilder> builders;

	public static GraphBuilder sequenceSimple =
		new GraphBuilder("sequence_simple") {
			@Override
			public void build(Graph g) {
				long start = 0;
				if (g.getTail(A) != null) {
					start = g.getTail(A).getTs();
				}
				long end = 10 + start;
				for (long i = start + 1; i < end; i++)
					g.append(A, new Node(i));
			}
		};

	public TestGraphFactory() {
		builders = new HashMap<String, GraphBuilder>();
		Field[] fields = getClass().getDeclaredFields();
		for (Field f: fields) {
			if (!Modifier.isStatic(f.getModifiers()))
				continue;
			Object builder = null;
			try {
				builder = f.get(null);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			if (builder instanceof GraphBuilder) {
				registerBuilder((GraphBuilder) builder);
			}
		}
	}

	public void registerBuilder(GraphBuilder f) {
		builders.put(f.getName(), f);
	}

	public HashMap<String, GraphBuilder> getBuildersMap() {
		return builders;
	}

	public static void main(String[] args) throws IOException {
		File base = new File("results", TestGraphFactory.class.getName());
		base.mkdirs();
		TestGraphFactory factory = new TestGraphFactory();
		Collection<GraphBuilder> kind = factory.getBuildersMap().values();
		for (GraphBuilder builder: kind) {
			Graph g = new Graph();
			builder.build(g);
			String content = Dot.todot(g);
			TestGraph.writeString(factory, builder.getName() + ".dot", content);
		}
	}

}
