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
				data.head = Ops.basic(data.len, LinkType.RUNNING);
			}

			@Override
			public GraphBuilderData[] params() {
				int max = 1;
				GraphBuilderData[] data = new GraphBuilderData[max];
				for (int i = 0; i < max; i++) {
					data[i] = new GraphBuilderData();
					data[i].id = i;
					data[i].len = 2;
				}
				return data;
			}

			@Override
			public void criticalPathBounded(GraphBuilderData data) {
				data.bounded = makeit(data);
			}

			@Override
			public void criticalPathUnbounded(GraphBuilderData data) {
				data.unbounded = makeit(data);
			}

			private Node makeit(GraphBuilderData data) {
				return Ops.basic(data.len, LinkType.RUNNING);
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
			public GraphBuilderData[] params() {
				int max = 1;
				GraphBuilderData[] data = new GraphBuilderData[max];
				for (int i = 0; i < max; i++) {
					data[i] = new GraphBuilderData();
					data[i].id = i;
					data[i].len = 1;
				}
				return data;
			}

			@Override
			public void criticalPathBounded(GraphBuilderData data) {
				Node n1 = Ops.sequence(5, data.len, LinkType.RUNNING);
				Ops.seek(n1, 2).links[Node.RIGHT].type = LinkType.TIMER;
				data.bounded = n1;
			}

			@Override
			public void criticalPathUnbounded(GraphBuilderData data) {
				Node n1 = Ops.basic(data.len, LinkType.RUNNING);
				Node n2 = Ops.basic(data.len * 2, LinkType.TIMER);
				Node n3 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.concatInPlace(n1, n2);
				Ops.concatInPlace(n2, n3);
				data.unbounded = n1;
			}
		};

	public static String GRAPH_WAKEUP_MISSING = "wakeup_missing";
	public static GraphBuilder wakeupMissing =
		new GraphBuilder(GRAPH_WAKEUP_MISSING) {
			@Override
			public void build(GraphBuilderData data) {
				Node t1 = Ops.sequence(4, data.len, LinkType.RUNNING);
				Ops.seek(t1, 1).links[Node.RIGHT].type = LinkType.BLOCKED;
				data.head = t1;
			}

			@Override
			public GraphBuilderData[] params() {
				int max = 1;
				GraphBuilderData[] data = new GraphBuilderData[max];
				for (int i = 0; i < max; i++) {
					data[i] = new GraphBuilderData();
					data[i].id = i;
					data[i].len = 2;
				}
				return data;
			}

			@Override
			public void criticalPathBounded(GraphBuilderData data) {
				Node n1 = Ops.sequence(4, data.len, LinkType.RUNNING);
				Ops.seek(n1, 1).links[Node.RIGHT].type = LinkType.UNKNOWN;
				data.bounded = n1;
			}

			@Override
			public void criticalPathUnbounded(GraphBuilderData data) {
				data.unbounded = new Node(0);
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
			public GraphBuilderData[] params() {
				int max = 1;
				GraphBuilderData[] data = new GraphBuilderData[max];
				for (int i = 0; i < max; i++) {
					data[i] = new GraphBuilderData();
					data[i].id = i;
					data[i].len = 2;
					data[i].delay = 1;
				}
				return data;
			}

			@Override
			public void criticalPathBounded(GraphBuilderData data) {
				Node n1 = Ops.basic(data.len, LinkType.RUNNING);
				long duration = (data.len <= data.delay) ? 0 : data.len - data.delay;
				Node n2 = Ops.basic(duration, LinkType.UNKNOWN);
				Ops.offset(n2, data.len);
				Node n3 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.offset(n3, data.len * 2);
				Ops.tail(n1).linkVertical(n2);
				Ops.tail(n2).linkVertical(n3).type = LinkType.NETWORK;
				data.bounded = n1;
			}

			@Override
			public void criticalPathUnbounded(GraphBuilderData data) {
				Node n1 = Ops.basic(data.len, LinkType.RUNNING);
				Node n2 = Ops.basic(data.len - data.delay, LinkType.UNKNOWN);
				Node n3 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.concatInPlace(n1, n2);
				Ops.offset(n3, data.len * 2);
				Ops.tail(n2).linkVertical(n3).type = LinkType.NETWORK;
				data.unbounded = n1;
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
			public GraphBuilderData[] params() {
				int max = 5;
				GraphBuilderData[] data = new GraphBuilderData[max];
				for (int i = 0; i < max; i++) {
					data[i] = new GraphBuilderData();
					data[i].id = i;
					data[i].len = 2;
					data[i].delay = i;
				}
				return data;
			}

			@Override
			public void criticalPathBounded(GraphBuilderData data) {
				Node n1 = Ops.sequence(3, data.len, LinkType.RUNNING);

				long duration = (data.delay < data.len) ? data.len : (data.len * 2) - data.delay;
				Node n2 = Ops.basic(duration, LinkType.RUNNING);
				Ops.offset(n2, data.len * 2 + (data.len - duration));

				Node n3 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.offset(n3, data.len * 3);
				Ops.seek(n1, 2).linkVertical(n2);
				Ops.tail(n2).linkVertical(n3);
				data.bounded = n1;
			}

			@Override
			public void criticalPathUnbounded(GraphBuilderData data) {
				Node n1 = Ops.basic(data.len, LinkType.RUNNING);
				Node n2 = Ops.basic(data.len * 2 - data.delay, LinkType.RUNNING);
				Node n3 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.offset(n2, data.len + data.delay);
				Ops.offset(n3, data.len * 3);
				Ops.tail(n1).linkVertical(n2);
				Ops.tail(n2).linkVertical(n3);
				data.unbounded = n1;
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
			public GraphBuilderData[] params() {
				int max = 5;
				GraphBuilderData[] data = new GraphBuilderData[max];
				for (int i = 0; i < max; i++) {
					data[i] = new GraphBuilderData();
					data[i].id = i;
					data[i].len = 3;
					data[i].delay = i;
				}
				return data;
			}

			@Override
			public void criticalPathBounded(GraphBuilderData data) {
				Node n1 = Ops.basic(data.len, LinkType.RUNNING);
				long duration = (data.len <= data.delay) ? 0 : data.len - data.delay;
				Node n2 = new Node(0);
				if (duration > 0)
					n2 = Ops.basic(duration, LinkType.RUNNING);
				Ops.offset(n2, data.len);
				Node n3 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.offset(n3, data.len * 2);
				Ops.tail(n1).linkVertical(n2);
				Ops.tail(n2).linkVertical(n3);
				data.bounded = n1;
			}

			@Override
			public void criticalPathUnbounded(GraphBuilderData data) {
				data.unbounded = new Node(0);
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
			public GraphBuilderData[] params() {
				int max = 1;
				GraphBuilderData[] data = new GraphBuilderData[max];
				for (int i = 0; i < max; i++) {
					data[i] = new GraphBuilderData();
					data[i].id = i;
					data[i].len = 1;
				}
				return data;
			}

			@Override
			public void criticalPathBounded(GraphBuilderData data) {
				Node n1 = Ops.sequence(4, data.len, LinkType.RUNNING);
				Node n2 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.offset(n2, Ops.tail(n1).getTs());
				Node n3 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.offset(n3, Ops.tail(n2).getTs());
				Ops.tail(n1).linkVertical(n2);
				Ops.tail(n2).linkVertical(n3);
				data.bounded = n1;
			}

			@Override
			public void criticalPathUnbounded(GraphBuilderData data) {
				Node n1 = Ops.sequence(3, data.len, LinkType.RUNNING);
				Node n2 = Ops.sequence(3, data.len, LinkType.RUNNING);
				Node n3 = Ops.basic(data.len, LinkType.RUNNING);
				Ops.offset(n2, Ops.tail(n1).getTs());
				Ops.offset(n3, Ops.tail(n2).getTs());
				Ops.tail(n1).linkVertical(n2);
				Ops.tail(n2).linkVertical(n3);
				data.unbounded = n1;
			}
		};

	public static String GRAPH_WAKEUP_EMBEDED = "wakeup_embeded";
	public static GraphBuilder wakeupEmbeded =
		new GraphBuilder(GRAPH_WAKEUP_EMBEDED) {
			@Override
			public void build(GraphBuilderData data) {
				Node t1 = Ops.sequence(3, data.len, LinkType.RUNNING);
				for (int i = 0; i < data.depth; i++) {
					Ops.tail(t1).links[Node.LEFT].type = LinkType.BLOCKED;
					long duration = Ops.tail(t1).getTs() - t1.getTs();
					Node sub = Ops.basic(duration, LinkType.RUNNING);
					Ops.unionInPlace(t1, sub, LinkType.DEFAULT, LinkType.DEFAULT);
					Node x1 = Ops.basic(data.len, LinkType.RUNNING);
					Node x2 = Ops.basic(data.len, LinkType.RUNNING);
					Ops.concatInPlace(x1, t1);
					Ops.concatInPlace(t1, x2);
					t1 = x1;
				}
				data.head = t1;
			}

			@Override
			public GraphBuilderData[] params() {
				int max = 3;
				GraphBuilderData[] data = new GraphBuilderData[max];
				for (int i = 0; i < max; i++) {
					data[i] = new GraphBuilderData();
					data[i].id = i;
					data[i].len = 2;
					data[i].depth = i;
				}
				return data;
			}

			@Override
			public void criticalPathBounded(GraphBuilderData data) {
				Node n1 = Ops.sequence(data.depth + 2, data.len, LinkType.RUNNING);
				Node curr = n1;
				for (int i = 0; i < data.depth; i++) {
					Node sub = Ops.basic(data.len, LinkType.RUNNING);
					Ops.offset(sub, Ops.tail(curr).getTs());
					Ops.tail(curr).linkVertical(sub);
					Node x = new Node(Ops.tail(sub));
					Ops.tail(sub).linkVertical(x);
					curr = x;
				}
				Ops.concatInPlace(curr, Ops.basic(data.len, LinkType.RUNNING));
				data.bounded = n1;
			}

			@Override
			public void criticalPathUnbounded(GraphBuilderData data) {
				data.unbounded = new Node(0);
			}
		};

	public static String GRAPH_WAKEUP_INTERLEAVE = "wakeup_interleave";
	public static GraphBuilder wakeupInterleave =
		new GraphBuilder(GRAPH_WAKEUP_INTERLEAVE) {
			@Override
			public void build(GraphBuilderData data) {
				Node t1 = Ops.sequence(3 + 2 * data.depth, data.len, LinkType.RUNNING);
				for (int i = 0; i < data.depth; i++) {
					Node x = Ops.seek(t1, i + 1);
					Node y = Ops.seek(t1, i + 4);
					y.links[Node.LEFT].type = LinkType.BLOCKED;
					Node sub = Ops.basic(3 * data.len, LinkType.RUNNING);
					Ops.offset(sub, x.getTs());
					x.linkVertical(sub);
					Ops.tail(sub).linkVertical(y);
				}
				Ops.concatInPlace(t1, Ops.basic(data.len, LinkType.RUNNING));
				data.head = t1;
			}

			@Override
			public GraphBuilderData[] params() {
				int max = 3;
				GraphBuilderData[] data = new GraphBuilderData[max];
				for (int i = 0; i < max; i++) {
					data[i] = new GraphBuilderData();
					data[i].id = i;
					data[i].len = 2;
					data[i].depth = i;
				}
				return data;
			}

			@Override
			public void criticalPathBounded(GraphBuilderData data) {
				int n = data.depth == 0 ? 3 : 4;
				Node n1 = Ops.sequence(n, data.len, LinkType.RUNNING);
				Node curr = n1;
				for (int i = 0; i < data.depth; i++) {
					Node sub = Ops.basic(data.len, LinkType.RUNNING);
					Ops.offset(sub, Ops.tail(curr).getTs());
					Ops.tail(curr).linkVertical(sub);
					Node x = new Node(Ops.tail(sub));
					Ops.tail(sub).linkVertical(x);
					curr = x;
				}
				Ops.concatInPlace(curr, Ops.basic(data.len, LinkType.RUNNING));
				// FIXME: should replace this with proper general relationship
				if (data.depth == 2) {
					Ops.concatInPlace(curr, Ops.basic(data.len, LinkType.RUNNING));
				}
				data.bounded = n1;
			}

			@Override
			public void criticalPathUnbounded(GraphBuilderData data) {
				data.unbounded = new Node(0);
			}
		};

	public static String GRAPH_NESTED = "wakeup_nested";
	public static GraphBuilder wakeupNested =
		new GraphBuilder(GRAPH_NESTED) {
			@Override
			public void build(GraphBuilderData data) {
				Node inner = Ops.basic(data.len * 2, LinkType.RUNNING);
				for (int i = 0; i < data.depth; i++) {
					long duration = Ops.tail(inner).getTs() - Ops.head(inner).getTs();
					Node t1 = Ops.basic(data.len, LinkType.RUNNING);
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
			public GraphBuilderData[] params() {
				int max = 4;
				GraphBuilderData[] data = new GraphBuilderData[max];
				for (int i = 0; i < max; i++) {
					data[i] = new GraphBuilderData();
					data[i].id = i;
					data[i].len = 1;
					data[i].depth = i;
				}
				return data;
			}

			@Override
			public void criticalPathBounded(GraphBuilderData data) {
				Node inner = Ops.basic(data.len * 2, LinkType.RUNNING);
				Node innerOrig = inner;
				for (int i = 0; i < data.depth; i++) {
					Node t1 = Ops.basic(data.len, LinkType.RUNNING);
					Ops.offset(inner, data.len);
					Ops.tail(t1).linkVertical(inner);
					inner = t1;
				}
				data.bounded = inner;
				inner = innerOrig;
				for (int i = 0; i < data.depth; i++) {
					Node t1 = Ops.basic(data.len, LinkType.RUNNING);
					Ops.offset(t1, Ops.tail(inner).getTs());
					Ops.tail(inner).linkVertical(t1);
					inner = t1;
				}
			}

			@Override
			public void criticalPathUnbounded(GraphBuilderData data) {
				if (data.bounded == null)
					criticalPathBounded(data);
				data.unbounded = data.bounded;
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

	public GraphBuilder get(String name) {
		return builders.get(name);
	}

	public static void main(String[] args) throws IOException {
		File base = new File("results", GraphFactory.class.getName());
		base.mkdirs();
		GraphFactory factory = new GraphFactory();
		Collection<GraphBuilder> kind = factory.getBuildersMap().values();
		for (GraphBuilder builder: kind) {
			System.out.println("processing " + builder.getName());
			GraphBuilderData[] params = builder.params();
			for (GraphBuilderData data: params) {
				builder.build(data);
				builder.criticalPathBounded(data);
				builder.criticalPathUnbounded(data);
				String graph = Dot.todot(Ops.head(data.head));
				String bounded = Dot.todot(Ops.head(data.bounded));
				String unbounded = Dot.todot(Ops.head(data.unbounded));
				Dot.writeString(factory.getClass(), builder.getName() + "_" + data.id + "_graph.dot", graph);
				Dot.writeString(factory.getClass(), builder.getName() + "_" + data.id + "_bounded.dot", bounded);
				Dot.writeString(factory.getClass(), builder.getName() + "_" + data.id + "_unbounded.dot", unbounded);
			}
		}
	}

}
