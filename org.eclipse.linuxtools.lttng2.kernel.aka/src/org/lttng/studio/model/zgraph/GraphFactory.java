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
			public void build(GraphBuilderData state) {
				state.head = Ops.concat(state.head, Ops.basic(state.len));
			}

			@Override
			public GraphBuilderData getDefaults() {
				GraphBuilderData data = new GraphBuilderData();
				data.len = 1;
				return data;
			}
		};

	public static String GRAPH_PREEMPT = "preempt";
	public static GraphBuilder preempt =
		new GraphBuilder(GRAPH_PREEMPT) {
			@Override
			public void build(GraphBuilderData state) {
				Node n = Ops.basic(state.len, LinkType.PREEMPTED);
				state.head = Ops.concat(state.head, n);
			}

			@Override
			public GraphBuilderData getDefaults() {
				GraphBuilderData data = new GraphBuilderData();
				data.len = 1;
				return data;
			}
		};

	public static String GRAPH_STATE_SEQUENCE = "state_squence";
	public static GraphBuilder state_sequence =
		new GraphBuilder(GRAPH_STATE_SEQUENCE) {
			@Override
			public void build(GraphBuilderData state) {
				Node n = null;
				int idx = 0;
				for (int i = 0; i < state.num; i++) {
					if (n == null)
						n = new Node(i);
					Node next = new Node((i + 1) * state.len);
					Link link = n.linkHorizontal(next);
					link.type = state.types.get(idx);
					idx = (idx + 1) % state.types.size();
					n = next;
				}
				state.head = Ops.concat(state.head, n);
			}

			@Override
			public GraphBuilderData getDefaults() {
				GraphBuilderData data = new GraphBuilderData();
				data.len = 1;
				data.types.add(LinkType.RUNNING);
				data.types.add(LinkType.PREEMPTED);
				data.types.add(LinkType.BLOCKED);
				data.num = data.types.size() * 2;
				return data;
			}
		};

	public static String GRAPH_WAKEUP_INSTANT = "wakeup_instant";
	public static GraphBuilder wakeupInstant =
		new GraphBuilder(GRAPH_WAKEUP_INSTANT) {
			@Override
			public void build(GraphBuilderData data) {
				Node sub = Ops.basic(data.len);
				sub.links[Node.RIGHT].type = LinkType.RUNNING;
				Ops.alignRight(data.head, sub);
				Ops.unionInPlaceRight(data.head, sub, LinkType.DEFAULT);
			}

			@Override
			public GraphBuilderData getDefaults() {
				GraphBuilderData data = new GraphBuilderData();
				data.len = 1;
				return data;
			}
		};

	public static String GRAPH_WAKEUP_DELAY = "wakeup_delay";
	public static GraphBuilder wakeupDelay =
		new GraphBuilder("wakeup_delay") {
			@Override
			public void build(GraphBuilderData data) {
				Node sub = Ops.basic(data.len);
				sub.links[Node.RIGHT].type = LinkType.RUNNING;
				Ops.alignRight(data.head, sub);
				Ops.offset(sub, -data.len);
				Ops.unionInPlaceRight(data.head, sub, LinkType.DEFAULT);
			}

			@Override
			public GraphBuilderData getDefaults() {
				GraphBuilderData data = new GraphBuilderData();
				data.len = 1;
				return data;
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
			GraphBuilderData data = builder.getDefaults();
			builder.build(data);
			String content = Dot.todot(Ops.head(data.head));
			Dot.writeString(factory, builder.getName() + ".dot", content);
		}
	}

}
