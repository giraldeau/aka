package org.lttng.studio.tests.graph;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

import org.lttng.studio.model.graph.EdgeType;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecGraph;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.utils.GraphUtils;

import com.google.common.collect.ArrayListMultimap;

public class BasicGraph {

	public static String GRAPH_BASIC 		= "basic";
	public static String GRAPH_CONCAT 		= "concat";
	public static String GRAPH_EMBEDED 		= "embeded";
	public static String GRAPH_INTERLEAVE 	= "interleave";
	public static String GRAPH_NESTED 		= "nested";
	public static String GRAPH_OPEN1 		= "open_1";
	public static String GRAPH_OPEN2 		= "open_2";
	public static String GRAPH_GARBAGE1 	= "garbage1";
	public static String GRAPH_GARBAGE2 	= "garbage2";
	public static String GRAPH_GARBAGE3 	= "garbage3";
	public static String GRAPH_SHELL 		= "shell";

	private static HashMap<String, Method> func = new HashMap<String, Method>();
	static {
		try {
			func.put(GRAPH_BASIC, 		BasicGraph.class.getDeclaredMethod("makeBasic"));
			func.put(GRAPH_CONCAT, 		BasicGraph.class.getDeclaredMethod("makeConcat"));
			func.put(GRAPH_EMBEDED, 	BasicGraph.class.getDeclaredMethod("makeEmbeded"));
			func.put(GRAPH_INTERLEAVE,	BasicGraph.class.getDeclaredMethod("makeInterleave"));
			func.put(GRAPH_NESTED, 		BasicGraph.class.getDeclaredMethod("makeNested"));
			func.put(GRAPH_OPEN1, 		BasicGraph.class.getDeclaredMethod("makeOpened1"));
			func.put(GRAPH_OPEN2, 		BasicGraph.class.getDeclaredMethod("makeOpened2"));
			func.put(GRAPH_GARBAGE1,	BasicGraph.class.getDeclaredMethod("makeGarbage1"));
			func.put(GRAPH_GARBAGE2,	BasicGraph.class.getDeclaredMethod("makeGarbage2"));
			func.put(GRAPH_GARBAGE3,	BasicGraph.class.getDeclaredMethod("makeGarbage3"));
			func.put(GRAPH_SHELL, 		BasicGraph.class.getDeclaredMethod("makeExecShell"));
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	};

	public static ExecGraph makeLengthUnequal() {
		ExecGraph graph = makeGraph();
		Object A = "A";
		Object B = "B";

		ExecVertex vA0 = new ExecVertex(A, 0);
		ExecVertex vA2 = new ExecVertex(A, 2);

		ExecVertex vB0 = new ExecVertex(B, 0);
		ExecVertex vB1 = new ExecVertex(B, 1);
		ExecVertex vB2 = new ExecVertex(B, 2);
		ExecVertex vB3 = new ExecVertex(B, 3);

		graph.appendVertexByOwner(vA0);
		graph.appendVertexByOwner(vA2);
		graph.appendVertexByOwner(vB0);
		graph.appendVertexByOwner(vB1);
		graph.appendVertexByOwner(vB2);
		graph.appendVertexByOwner(vB3);

		graph.addVerticalEdge(vA0, vB0, EdgeType.SPLIT);
		graph.addVerticalEdge(vB2, vA2, EdgeType.MERGE);
		return graph;
	}

	public static ExecGraph makeBasic() {
		ExecGraph graph = makeGraph();
		Object A = "A";
		Object B = "B";

		ExecVertex[] vA = genSeq(graph, A, 4);

		ExecVertex vB1 = new ExecVertex(B, 1);
		ExecVertex vB2 = new ExecVertex(B, 2);
		graph.appendVertexByOwner(vB1);
		graph.appendVertexByOwner(vB2);

		graph.addVerticalEdge(vA[1], vB1, EdgeType.SPLIT);
		graph.addVerticalEdge(vB2, vA[2], EdgeType.MERGE);
		setEdgeBlocked(graph, "A1", "A2");
		return graph;
	}

	public static ExecGraph makeConcat() {
		ExecGraph graph = makeGraph();
		Object A = "A";
		Object B = "B";
		Object C = "C";

		ExecVertex[] vA = genSeq(graph, A, 6);

		ExecVertex vB1 = new ExecVertex(B, 1);
		ExecVertex vB2 = new ExecVertex(B, 2);
		graph.appendVertexByOwner(vB1);
		graph.appendVertexByOwner(vB2);

		ExecVertex vC3 = new ExecVertex(C, 3);
		ExecVertex vC4 = new ExecVertex(C, 4);
		graph.appendVertexByOwner(vC3);
		graph.appendVertexByOwner(vC4);

		graph.addVerticalEdge(vA[1], vB1, EdgeType.SPLIT);
		graph.addVerticalEdge(vB2, vA[2], EdgeType.MERGE);

		graph.addVerticalEdge(vA[3], vC3, EdgeType.SPLIT);
		graph.addVerticalEdge(vC4, vA[4], EdgeType.MERGE);
		setEdgeBlocked(graph, "A1", "A2");
		setEdgeBlocked(graph, "A3", "A4");
		return graph;
	}

	public static ExecGraph makeInterleave() {
		ExecGraph graph = makeGraph();
		Object A = "A";
		Object B = "B";
		Object C = "C";

		ExecVertex[] vA = genSeq(graph, A, 6);

		ExecVertex vB1 = new ExecVertex(B, 1);
		ExecVertex vB3 = new ExecVertex(B, 3);
		graph.appendVertexByOwner(vB1);
		graph.appendVertexByOwner(vB3);

		ExecVertex vC2 = new ExecVertex(C, 2);
		ExecVertex vC4 = new ExecVertex(C, 4);
		graph.appendVertexByOwner(vC2);
		graph.appendVertexByOwner(vC4);

		graph.addVerticalEdge(vA[1], vB1, EdgeType.SPLIT);
		graph.addVerticalEdge(vB3, vA[3], EdgeType.MERGE);

		graph.addVerticalEdge(vA[2], vC2, EdgeType.SPLIT);
		graph.addVerticalEdge(vC4, vA[4], EdgeType.MERGE);
		setEdgeBlocked(graph, "A2", "A3");
		setEdgeBlocked(graph, "A3", "A4");
		return graph;
	}

	public static ExecGraph makeEmbeded() {
		ExecGraph graph = makeGraph();
		Object A = "A";
		Object B = "B";
		Object C = "C";

		ExecVertex[] vA = genSeq(graph, A, 6);

		ExecVertex vB2 = new ExecVertex(B, 2);
		ExecVertex vB3 = new ExecVertex(B, 3);
		graph.appendVertexByOwner(vB2);
		graph.appendVertexByOwner(vB3);

		ExecVertex vC1 = new ExecVertex(C, 1);
		ExecVertex vC4 = new ExecVertex(C, 4);
		graph.appendVertexByOwner(vC1);
		graph.appendVertexByOwner(vC4);

		graph.addVerticalEdge(vA[2], vB2, EdgeType.SPLIT);
		graph.addVerticalEdge(vB3, vA[3], EdgeType.MERGE);

		graph.addVerticalEdge(vA[1], vC1, EdgeType.SPLIT);
		graph.addVerticalEdge(vC4, vA[4], EdgeType.MERGE);
		setEdgeBlocked(graph, "A2", "A3");
		setEdgeBlocked(graph, "A3", "A4");
		return graph;
	}

	public static ExecGraph makeNested() {
		ExecGraph graph = makeGraph();
		Object A = "A";
		Object B = "B";
		Object C = "C";

		ExecVertex vA0 = new ExecVertex(A, 0);
		ExecVertex vA1 = new ExecVertex(A, 1);
		ExecVertex vA4 = new ExecVertex(A, 4);
		ExecVertex vA5 = new ExecVertex(A, 5);
		graph.appendVertexByOwner(vA0);
		graph.appendVertexByOwner(vA1);
		graph.appendVertexByOwner(vA4);
		graph.appendVertexByOwner(vA5);

		ExecVertex vB1 = new ExecVertex(B, 1);
		ExecVertex vB2 = new ExecVertex(B, 2);
		ExecVertex vB3 = new ExecVertex(B, 3);
		ExecVertex vB4 = new ExecVertex(B, 4);
		graph.appendVertexByOwner(vB1);
		graph.appendVertexByOwner(vB2);
		graph.appendVertexByOwner(vB3);
		graph.appendVertexByOwner(vB4);

		ExecVertex vC2 = new ExecVertex(C, 2);
		ExecVertex vC3 = new ExecVertex(C, 3);
		graph.appendVertexByOwner(vC2);
		graph.appendVertexByOwner(vC3);

		graph.addVerticalEdge(vA1, vB1, EdgeType.SPLIT);
		graph.addVerticalEdge(vB2, vC2, EdgeType.SPLIT);

		graph.addVerticalEdge(vC3, vB3, EdgeType.MERGE);
		graph.addVerticalEdge(vB4, vA4, EdgeType.MERGE);
		setEdgeBlocked(graph, "A1", "A4");
		setEdgeBlocked(graph, "B2", "B3");
		return graph;
	}

	public static ExecGraph makeOpened1() {
		ExecGraph graph = makeGraph();
		Object A = "A";
		Object B = "B";

		ExecVertex vA0 = new ExecVertex(A, 0);
		ExecVertex vA1 = new ExecVertex(A, 1);
		ExecVertex vA2 = new ExecVertex(A, 2);
		graph.appendVertexByOwner(vA0);
		graph.appendVertexByOwner(vA1);
		graph.appendVertexByOwner(vA2);

		ExecVertex vB1 = new ExecVertex(B, 1);
		ExecVertex vB2 = new ExecVertex(B, 2);
		graph.appendVertexByOwner(vB1);
		graph.appendVertexByOwner(vB2);

		graph.addVerticalEdge(vA1, vB1, EdgeType.SPLIT);
		return graph;
	}

	public static ExecGraph makeOpened2() {
		ExecGraph graph = makeGraph();
		Object A = "A";
		Object B = "B";

		ExecVertex vA0 = new ExecVertex(A, 0);
		ExecVertex vA1 = new ExecVertex(A, 1);
		ExecVertex vA2 = new ExecVertex(A, 2);
		graph.appendVertexByOwner(vA0);
		graph.appendVertexByOwner(vA1);
		graph.appendVertexByOwner(vA2);

		ExecVertex vB0 = new ExecVertex(B, 0);
		ExecVertex vB1 = new ExecVertex(B, 1);
		graph.appendVertexByOwner(vB0);
		graph.appendVertexByOwner(vB1);

		graph.addVerticalEdge(vB1, vA1, EdgeType.MERGE);
		return graph;
	}

	public static ExecGraph makeGarbage1() {
		ExecGraph graph = makeGraph();
		Object A = "A";
		Object B = "B";
		Object C = "C";

		ExecVertex vA0 = new ExecVertex(A, 0);
		ExecVertex vA1 = new ExecVertex(A, 1);
		ExecVertex vA2 = new ExecVertex(A, 2);
		ExecVertex vA3 = new ExecVertex(A, 3);
		graph.appendVertexByOwner(vA0);
		graph.appendVertexByOwner(vA1);
		graph.appendVertexByOwner(vA2);
		graph.appendVertexByOwner(vA3);

		ExecVertex vB1 = new ExecVertex(B, 1);
		ExecVertex vB2 = new ExecVertex(B, 2);
		graph.appendVertexByOwner(vB1);
		graph.appendVertexByOwner(vB2);

		ExecVertex vC2 = new ExecVertex(C, 2);
		ExecVertex vC3 = new ExecVertex(C, 3);
		graph.appendVertexByOwner(vC2);
		graph.appendVertexByOwner(vC3);

		graph.addVerticalEdge(vA1, vB1, EdgeType.SPLIT);
		graph.addVerticalEdge(vB2, vA2, EdgeType.MERGE);
		graph.addVerticalEdge(vA3, vC3, EdgeType.MERGE);

		setEdgeBlocked(graph, "A1", "A2");

		return graph;
	}

	public static ExecGraph makeGarbage2() {
		ExecGraph graph = makeGraph();
		Object A = "A";
		Object B = "B";
		Object C = "C";

		ExecVertex vA0 = new ExecVertex(A, 0);
		ExecVertex vA1 = new ExecVertex(A, 1);
		ExecVertex vA2 = new ExecVertex(A, 2);
		ExecVertex vA3 = new ExecVertex(A, 3);
		graph.appendVertexByOwner(vA0);
		graph.appendVertexByOwner(vA1);
		graph.appendVertexByOwner(vA2);
		graph.appendVertexByOwner(vA3);

		ExecVertex vB1 = new ExecVertex(B, 1);
		ExecVertex vB2 = new ExecVertex(B, 2);
		graph.appendVertexByOwner(vB1);
		graph.appendVertexByOwner(vB2);

		ExecVertex vC2 = new ExecVertex(C, 2);
		ExecVertex vC3 = new ExecVertex(C, 3);
		ExecVertex vC4 = new ExecVertex(C, 4);
		graph.appendVertexByOwner(vC2);
		graph.appendVertexByOwner(vC3);
		graph.appendVertexByOwner(vC4);

		graph.addVerticalEdge(vA1, vB1, EdgeType.SPLIT);
		graph.addVerticalEdge(vB2, vA2, EdgeType.MERGE);
		graph.addVerticalEdge(vA3, vC3, EdgeType.MERGE);

		setEdgeBlocked(graph, "A1", "A2");

		return graph;
	}

	public static ExecGraph makeGarbage3() {
		ExecGraph graph = makeGraph();
		Object A = "A";
		Object B = "B";
		Object C = "C";

		ExecVertex vA0 = new ExecVertex(A, 0);
		ExecVertex vA1 = new ExecVertex(A, 1);
		ExecVertex vA2 = new ExecVertex(A, 2);
		ExecVertex vA3 = new ExecVertex(A, 3);
		ExecVertex vA4 = new ExecVertex(A, 4);
		graph.appendVertexByOwner(vA0);
		graph.appendVertexByOwner(vA1);
		graph.appendVertexByOwner(vA2);
		graph.appendVertexByOwner(vA3);
		graph.appendVertexByOwner(vA4);

		ExecVertex vB1 = new ExecVertex(B, 1);
		ExecVertex vB2 = new ExecVertex(B, 2);
		graph.appendVertexByOwner(vB1);
		graph.appendVertexByOwner(vB2);

		ExecVertex vC2 = new ExecVertex(C, 2);
		ExecVertex vC3 = new ExecVertex(C, 3);
		ExecVertex vC4 = new ExecVertex(C, 4);
		graph.appendVertexByOwner(vC2);
		graph.appendVertexByOwner(vC3);
		graph.appendVertexByOwner(vC4);

		graph.addVerticalEdge(vA1, vB1, EdgeType.SPLIT);
		graph.addVerticalEdge(vB2, vA2, EdgeType.MERGE);
		graph.addVerticalEdge(vA3, vC3, EdgeType.MERGE);

		setEdgeBlocked(graph, "A1", "A2");

		return graph;
	}

	public static ExecGraph makeExecShell() {
		ExecGraph graph = makeGraph();
		Object A = "A";
		Object B = "B";
		Object C = "C";
		Object D = "D";
		Object E = "E";

		ExecVertex[] vA = genSeq(graph, A, 19);
		ExecVertex[] vB = genSeq(graph, B, 19);
		ExecVertex[] vC = genSeq(graph, C, 19);
		ExecVertex[] vD = genSeq(graph, D, 19);
		ExecVertex[] vE = genSeq(graph, E, 19);

		// first shell
		graph.addVerticalEdge(vA[1], vB[1], EdgeType.SPLIT);
		graph.addVerticalEdge(vE[13], vA[13], EdgeType.MERGE);
		graph.addVerticalEdge(vB[17], vA[17], EdgeType.MERGE);

		// second shell
		graph.addVerticalEdge(vB[2], vC[2], EdgeType.SPLIT);
		graph.addVerticalEdge(vB[3], vD[3], EdgeType.SPLIT);
		graph.addVerticalEdge(vB[5], vE[5], EdgeType.SPLIT);

		// child 1
		graph.addVerticalEdge(vC[7], vD[7], EdgeType.SPLIT);
		graph.addVerticalEdge(vC[8], vB[8], EdgeType.MERGE);

		// child 2
		graph.addVerticalEdge(vD[10], vE[10], EdgeType.SPLIT);
		graph.addVerticalEdge(vD[11], vB[11], EdgeType.MERGE);

		// child 3
		graph.addVerticalEdge(vE[15], vB[15], EdgeType.MERGE);

		// blocking edges
		setEdgeBlocked(graph, "A2", "A3");
		setEdgeBlocked(graph, "A16", "A17");
		setEdgeBlocked(graph, "B6", "B7");
		setEdgeBlocked(graph, "B9", "B10");
		setEdgeBlocked(graph, "B12", "B13");
		setEdgeBlocked(graph, "D4", "D5");
		setEdgeBlocked(graph, "E6", "E7");
		return graph;
	}

	public static ExecGraph makeGraphByName(String name) {
		Method method = func.get(name);
		if (method == null)
			return null;
		try {
			return (ExecGraph) method.invoke(null);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ExecVertex getVertexByPrefix(ExecGraph graph, String prefix) {
		ExecVertex vertex = null;
		ArrayListMultimap<Object, ExecVertex> vertexMap = graph.getVertexMap();
		for (ExecVertex v : vertexMap.values()) {
			if (((String)v.getOwner()).startsWith(prefix)) {
				vertex = v;
				break;
			}
		}
		return vertex;
	}

	public static ExecVertex getVertexByName(ExecGraph graph, String name) {
		ExecVertex base = null;
		ArrayListMultimap<Object, ExecVertex> vertexMap = graph.getVertexMap();
		for (ExecVertex v : vertexMap.values()) {
			String s = "" + v.getOwner() + v.getTimestamp();
			if (s.compareTo(name) == 0)
				base = v;
		}
		return base;
	}

	public static ExecEdge getEdgeByName(ExecGraph graph, String s1, String s2) {
		ExecVertex v1 = getVertexByName(graph, s1);
		ExecVertex v2 = getVertexByName(graph, s2);
		return graph.getGraph().getEdge(v1, v2);
	}

	public static Set<String> getGraphName() {
		return func.keySet();
	}

	private static ExecVertex[] genSeq(ExecGraph graph, Object owner, int num) {
		ExecVertex[] v = new ExecVertex[num];
		for (int i = 0; i < num; i++) {
			v[i] = new ExecVertex(owner, i);
			graph.appendVertexByOwner(v[i]);
		}
		return v;
	}

	private static ExecGraph makeGraph() {
		return new ExecGraph();
	}

	private static void setEdgeBlocked(ExecGraph graph, String s1, String s2) {
		graph.getGraph()
		.getEdge(getVertexByName(graph, s1), getVertexByName(graph, s2))
		.setType(EdgeType.BLOCKED);
	}

	public static void main(String[] args) throws IOException {
		String base = "graph" + File.separator + "tests" + File.separator;
		for (String name: func.keySet()) {
			GraphUtils.saveGraphDefault(makeGraphByName(name), base + name);
		}
	}

}
