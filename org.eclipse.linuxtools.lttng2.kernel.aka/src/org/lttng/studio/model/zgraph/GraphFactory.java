package org.lttng.studio.model.zgraph;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;

public class GraphFactory {

	private final HashMap<String, GraphBuilder> builders;

	public static String GRAPH_BASIC = "basic";
	public static GraphBuilder basic =
		new GraphBuilder(GRAPH_BASIC) {
			@Override
			public void build(GraphBuilderData data) {
				data.head = Ops.basic(data.len);
			}

			@Override
			public GraphBuilderData getDefaults() {
				GraphBuilderData data = new GraphBuilderData();
				data.len = 1;
				return data;
			}

			@Override
			public void criticalPath(GraphBuilderData data) {
				data.path = Ops.basic(data.len);
			}
		};

	public static String GRAPH_WAKEUP_SELF = "wakeup_self";
	public static GraphBuilder wakeupSelf =
		new GraphBuilder(GRAPH_WAKEUP_SELF) {
			@Override
			public void build(GraphBuilderData data) {
				Node t1 = Ops.sequence(5, data.len, LinkType.RUNNING);
				Node t2 = Ops.seek(t1, 1);
				Node t3 = Ops.seek(t1, 2);
				Node t4 = Ops.seek(t1, 3);
				t3.links[Node.RIGHT].type = LinkType.BLOCKED;
				t2.linkVertical(t4).type = LinkType.TIMER;
				data.head = t1;
			}

			@Override
			public GraphBuilderData getDefaults() {
				GraphBuilderData data = new GraphBuilderData();
				data.len = 1;
				return data;
			}

			@Override
			public void criticalPath(GraphBuilderData data) {
				Node n1 = Ops.sequence(4, data.len, LinkType.RUNNING);
				Ops.offset(n1, data.len);
				n1.setTs(0);
				Ops.seek(n1, 1).links[Node.RIGHT].type = LinkType.TIMER;
				data.path = n1;
			}
		};

	public static String GRAPH_WAKEUP_UNKNOWN = "wakeup_unknown";
	public static GraphBuilder wakeupUnknown =
		new GraphBuilder(GRAPH_WAKEUP_UNKNOWN) {
			@Override
			public void build(GraphBuilderData data) {
				Node t1 = Ops.sequence(4, data.len, LinkType.RUNNING);
				Ops.seek(t1, 1).links[Node.RIGHT].type = LinkType.BLOCKED;
				Node t2 = new Node(data.len * 2 - data.delay);
				t2.linkVertical(Ops.seek(t1, 2)).type = LinkType.NETWORK;
				data.head = t1;
			}

			@Override
			public GraphBuilderData getDefaults() {
				GraphBuilderData data = new GraphBuilderData();
				data.len = 2;
				data.delay = 1;
				return data;
			}

			@Override
			public void criticalPath(GraphBuilderData data) {
				Node n1 = Ops.basic(data.len, LinkType.RUNNING);
				long duration = (data.len <= data.delay) ? 0 : data.len - data.delay;
				Node n2 = Ops.basic(duration, LinkType.UNKNOWN);
				Ops.offset(n2, data.len);
				Node n3 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.offset(n3, data.len * 2);
				Ops.tail(n1).linkVertical(n2);
				Ops.tail(n2).linkVertical(n3).type = LinkType.NETWORK;
				data.path = n1;
			}
		};

	public static String GRAPH_WAKEUP_NEW = "wakeup_new";
	public static GraphBuilder wakeupNew =
		new GraphBuilder(GRAPH_WAKEUP_NEW) {
			@Override
			public void build(GraphBuilderData data) {
				Node t1 = Ops.sequence(5, data.len, LinkType.RUNNING);
				Ops.seek(t1, 2).links[Node.RIGHT].type = LinkType.BLOCKED;
				Node t2 = Ops.basic(data.len * 2 - data.delay, LinkType.RUNNING);
				Ops.offset(t2, data.len + data.delay);
				Ops.seek(t1, 1).linkVertical(t2).type = LinkType.DEFAULT;
				Ops.tail(t2).linkVertical(Ops.seek(t1, 3)).type = LinkType.DEFAULT;
				data.head = t1;
			}

			@Override
			public GraphBuilderData getDefaults() {
				GraphBuilderData data = new GraphBuilderData();
				data.len = 2;
				data.delay = 3;
				return data;
			}

			@Override
			public void criticalPath(GraphBuilderData data) {
				Node n1 = Ops.basic(data.len * 2, LinkType.RUNNING);
				long duration = ((data.len * 2) <= data.delay) ? 0 : (data.len * 2) - data.delay;
				Node n2 = Ops.basic(duration, LinkType.RUNNING);
				Ops.offset(n2, data.len + data.delay);
				Node n3 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.offset(n3, data.len * 3);
				Ops.tail(n1).linkVertical(n2);
				Ops.tail(n2).linkVertical(n3);
				data.path = n1;
			}
		};

	public static String GRAPH_OPENED = "opened";
	public static GraphBuilder opened =
		new GraphBuilder(GRAPH_OPENED) {
			@Override
			public void build(GraphBuilderData data) {
				Node t1 = Ops.sequence(3, data.len, LinkType.RUNNING);
				t1.links[Node.RIGHT].to.links[Node.RIGHT].type = LinkType.BLOCKED;
				Node t2 = Ops.basic(data.len * 2 - data.delay, LinkType.RUNNING);
				Ops.unionInPlaceRight(t1, t2, LinkType.DEFAULT);
				Node t3 = Ops.basic(data.len, LinkType.RUNNING);
				Node t4 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.concatInPlace(t2, t3);
				Ops.concatInPlace(t1, t4);
				data.head = t1;
			}

			@Override
			public GraphBuilderData getDefaults() {
				GraphBuilderData data = new GraphBuilderData();
				data.len = 3;
				data.delay = 2;
				return data;
			}

			@Override
			public void criticalPath(GraphBuilderData data) {
				Node n1 = Ops.basic(data.len, LinkType.RUNNING);
				long duration = (data.len <= data.delay) ? 0 : data.len - data.delay;
				Node n2 = Ops.basic(duration, LinkType.RUNNING);
				Ops.offset(n2, data.len);
				Node n3 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.offset(n3, data.len * 2);
				Ops.tail(n1).linkVertical(n2);
				Ops.tail(n2).linkVertical(n3);
				data.path = n1;
			}
		};

	public static String GRAPH_WAKEUP_MUTUAL = "wakeup_mutual";
	public static GraphBuilder wakeupMutual =
		new GraphBuilder(GRAPH_WAKEUP_MUTUAL) {
			@Override
			public void build(GraphBuilderData data) {
				Node t1 = Ops.sequence(6, data.len, LinkType.RUNNING);
				Node t2 = Ops.sequence(6, data.len, LinkType.RUNNING);
				Ops.seek(t1, 3).links[Node.RIGHT].type = LinkType.BLOCKED;
				Ops.seek(t2, 1).links[Node.RIGHT].type = LinkType.BLOCKED;
				Ops.seek(t1, 2).linkVertical(Ops.seek(t2, 2));
				Ops.seek(t2, 4).linkVertical(Ops.seek(t1, 4));
				data.head = t1;
			}

			@Override
			public GraphBuilderData getDefaults() {
				GraphBuilderData data = new GraphBuilderData();
				data.len = 1;
				return data;
			}

			@Override
			public void criticalPath(GraphBuilderData data) {
				Node n1 = Ops.basic(data.len * 3, LinkType.RUNNING);
				Node n2 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.offset(n2, Ops.tail(n1).getTs());
				Node n3 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.offset(n3, Ops.tail(n2).getTs());
				Ops.tail(n1).linkVertical(n2);
				Ops.tail(n2).linkVertical(n3);
				data.path = n1;
			}
		};

	public static String GRAPH_NESTED = "wakeup_nested";
	public static GraphBuilder wakeupNested =
		new GraphBuilder(GRAPH_NESTED) {
			@Override
			public void build(GraphBuilderData data) {
				Node inner = Ops.basic(data.len * 2, LinkType.RUNNING);
				for (int i = 0; i < data.depth; i++) {
					Node t1 = Ops.basic(data.len, LinkType.RUNNING);
					long duration = Ops.tail(inner).getTs() - Ops.head(inner).getTs();
					Node t2 = Ops.basic(duration, LinkType.BLOCKED);
					Node t3 = Ops.basic(data.len, LinkType.RUNNING);
					Ops.alignRight(t2, inner);
					Ops.unionInPlace(t2, inner, LinkType.DEFAULT, LinkType.DEFAULT);
					Ops.concatInPlace(t2, t3);
					Ops.concatInPlace(t1, t2);
					inner = t1;
				}
				data.head = inner;
			}

			@Override
			public GraphBuilderData getDefaults() {
				GraphBuilderData data = new GraphBuilderData();
				data.len = 1;
				data.depth = 3;
				return data;
			}

			@Override
			public void criticalPath(GraphBuilderData data) {
				Node inner = Ops.basic(data.len * 2, LinkType.RUNNING);
				Node innerOrig = inner;
				for (int i = 0; i < data.depth; i++) {
					Node t1 = Ops.basic(data.len, LinkType.RUNNING);
					Ops.offset(inner, data.len);
					Ops.tail(t1).linkVertical(inner);
					inner = t1;
				}
				data.path = inner;
				inner = innerOrig;
				for (int i = 0; i < data.depth; i++) {
					Node t1 = Ops.basic(data.len, LinkType.RUNNING);
					Ops.offset(t1, Ops.tail(inner).getTs());
					Ops.tail(inner).linkVertical(t1);
					inner = t1;
				}
			}
		};

	public GraphFactory() {
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
		File base = new File("results", GraphFactory.class.getName());
		base.mkdirs();
		GraphFactory factory = new GraphFactory();
		Collection<GraphBuilder> kind = factory.getBuildersMap().values();
		for (GraphBuilder builder: kind) {
			System.out.println("processing " + builder.getName());
			GraphBuilderData data = builder.getDefaults();
			builder.build(data);
			builder.criticalPath(data);
			String graph = Dot.todot(Ops.head(data.head));
			String path = Dot.todot(Ops.head(data.path));
			Dot.writeString(factory, builder.getName() + "_graph.dot", graph);
			Dot.writeString(factory, builder.getName() + "_path.dot", path);
		}
	}

}
