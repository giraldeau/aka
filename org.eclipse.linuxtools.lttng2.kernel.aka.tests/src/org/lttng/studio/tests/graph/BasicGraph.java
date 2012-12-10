package org.lttng.studio.tests.graph;

import java.io.File;
import java.io.IOException;

import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.utils.GraphUtils;

public class BasicGraph {

	public static DirectedWeightedMultigraph<ExecVertex, ExecEdge> makeBasic() {
		DirectedWeightedMultigraph<ExecVertex, ExecEdge> graph = makeGraph();
		Object A = "A";
		Object B = "B";

		ExecVertex[] vA = genSeq(graph, A, 4);

		ExecVertex vB1 = new ExecVertex(B, 1);
		ExecVertex vB2 = new ExecVertex(B, 2);
		graph.addVertex(vB1);
		graph.addVertex(vB2);

		graph.addEdge(vB1, vB2);
		graph.addEdge(vA[1], vB1);
		graph.addEdge(vB2, vA[2]);
		return graph;
	}
	
	public static DirectedWeightedMultigraph<ExecVertex, ExecEdge> makeConcat() {
		DirectedWeightedMultigraph<ExecVertex, ExecEdge> graph = makeGraph();
		Object A = "A";
		Object B = "B";
		Object C = "C";

		ExecVertex[] vA = genSeq(graph, A, 6);

		ExecVertex vB1 = new ExecVertex(B, 1);
		ExecVertex vB2 = new ExecVertex(B, 2);
		graph.addVertex(vB1);
		graph.addVertex(vB2);
		graph.addEdge(vB1, vB2);

		ExecVertex vC3 = new ExecVertex(C, 3);
		ExecVertex vC4 = new ExecVertex(C, 4);
		graph.addVertex(vC3);
		graph.addVertex(vC4);
		graph.addEdge(vC3, vC4);

		graph.addEdge(vA[1], vB1);
		graph.addEdge(vB2, vA[2]);

		graph.addEdge(vA[3], vC3);
		graph.addEdge(vC4, vA[4]);
		return graph;
	}

	public static DirectedWeightedMultigraph<ExecVertex, ExecEdge> makeInterleave() {
		DirectedWeightedMultigraph<ExecVertex, ExecEdge> graph = makeGraph();
		Object A = "A";
		Object B = "B";
		Object C = "C";

		ExecVertex[] vA = genSeq(graph, A, 6);

		ExecVertex vB1 = new ExecVertex(B, 1);
		ExecVertex vB3 = new ExecVertex(B, 3);
		graph.addVertex(vB1);
		graph.addVertex(vB3);
		graph.addEdge(vB1, vB3);

		ExecVertex vC2 = new ExecVertex(C, 2);
		ExecVertex vC4 = new ExecVertex(C, 4);
		graph.addVertex(vC2);
		graph.addVertex(vC4);
		graph.addEdge(vC2, vC4);

		graph.addEdge(vA[1], vB1);
		graph.addEdge(vB3, vA[3]);

		graph.addEdge(vA[2], vC2);
		graph.addEdge(vC4, vA[4]);
		return graph;
	}

	public static DirectedWeightedMultigraph<ExecVertex, ExecEdge> makeEmbeded() {
		DirectedWeightedMultigraph<ExecVertex, ExecEdge> graph = makeGraph();
		Object A = "A";
		Object B = "B";
		Object C = "C";

		ExecVertex[] vA = genSeq(graph, A, 6);

		ExecVertex vB2 = new ExecVertex(B, 2);
		ExecVertex vB3 = new ExecVertex(B, 3);
		graph.addVertex(vB2);
		graph.addVertex(vB3);
		graph.addEdge(vB2, vB3);

		ExecVertex vC1 = new ExecVertex(C, 1);
		ExecVertex vC4 = new ExecVertex(C, 4);
		graph.addVertex(vC1);
		graph.addVertex(vC4);
		graph.addEdge(vC1, vC4);

		graph.addEdge(vA[2], vB2);
		graph.addEdge(vB3, vA[3]);

		graph.addEdge(vA[1], vC1);
		graph.addEdge(vC4, vA[4]);
		return graph;
	}

	public static DirectedWeightedMultigraph<ExecVertex, ExecEdge> makeNested() {
		DirectedWeightedMultigraph<ExecVertex, ExecEdge> graph = makeGraph();
		Object A = "A";
		Object B = "B";
		Object C = "C";

		ExecVertex vA0 = new ExecVertex(A, 0);
		ExecVertex vA1 = new ExecVertex(A, 1);
		ExecVertex vA4 = new ExecVertex(A, 4);
		ExecVertex vA5 = new ExecVertex(A, 5);
		graph.addVertex(vA0);
		graph.addVertex(vA1);
		graph.addVertex(vA4);
		graph.addVertex(vA5);
		graph.addEdge(vA0, vA1);
		graph.addEdge(vA1, vA4);
		graph.addEdge(vA4, vA5);

		ExecVertex vB1 = new ExecVertex(B, 1);
		ExecVertex vB2 = new ExecVertex(B, 2);
		ExecVertex vB3 = new ExecVertex(B, 3);
		ExecVertex vB4 = new ExecVertex(B, 4);
		graph.addVertex(vB1);
		graph.addVertex(vB2);
		graph.addVertex(vB3);
		graph.addVertex(vB4);
		graph.addEdge(vB1, vB2);
		graph.addEdge(vB2, vB3);
		graph.addEdge(vB3, vB4);

		ExecVertex vC2 = new ExecVertex(C, 2);
		ExecVertex vC3 = new ExecVertex(C, 3);
		graph.addVertex(vC2);
		graph.addVertex(vC3);
		graph.addEdge(vC2, vC3);

		graph.addEdge(vA1, vB1);
		graph.addEdge(vB2, vC2);

		graph.addEdge(vC3, vB3);
		graph.addEdge(vB4, vA4);
		return graph;
	}

	public static DirectedWeightedMultigraph<ExecVertex, ExecEdge> makeOpened1() {
		DirectedWeightedMultigraph<ExecVertex, ExecEdge> graph = makeGraph();
		Object A = "A";
		Object B = "B";

		ExecVertex vA0 = new ExecVertex(A, 0);
		ExecVertex vA1 = new ExecVertex(A, 1);
		ExecVertex vA2 = new ExecVertex(A, 2);
		graph.addVertex(vA0);
		graph.addVertex(vA1);
		graph.addVertex(vA2);
		graph.addEdge(vA0, vA1);
		graph.addEdge(vA1, vA2);

		ExecVertex vB1 = new ExecVertex(B, 1);
		ExecVertex vB2 = new ExecVertex(B, 2);
		graph.addVertex(vB1);
		graph.addVertex(vB2);
		graph.addEdge(vB1, vB2);

		graph.addEdge(vA1, vB1);
		return graph;
	}

	public static DirectedWeightedMultigraph<ExecVertex, ExecEdge> makeOpened2() {
		DirectedWeightedMultigraph<ExecVertex, ExecEdge> graph = makeGraph();
		Object A = "A";
		Object B = "B";

		ExecVertex vA0 = new ExecVertex(A, 0);
		ExecVertex vA1 = new ExecVertex(A, 1);
		ExecVertex vA2 = new ExecVertex(A, 2);
		graph.addVertex(vA0);
		graph.addVertex(vA1);
		graph.addVertex(vA2);
		graph.addEdge(vA0, vA1);
		graph.addEdge(vA1, vA2);

		ExecVertex vB0 = new ExecVertex(B, 0);
		ExecVertex vB1 = new ExecVertex(B, 1);
		graph.addVertex(vB0);
		graph.addVertex(vB1);
		graph.addEdge(vB0, vB1);

		graph.addEdge(vB1, vA1);
		return graph;
	}

	public static DirectedWeightedMultigraph<ExecVertex, ExecEdge> makeExecShell() {
		DirectedWeightedMultigraph<ExecVertex, ExecEdge> graph = makeGraph();
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
		graph.addEdge(vA[1], vB[1]);
		graph.addEdge(vE[13], vA[13]);
		graph.addEdge(vB[17], vA[17]);
		
		// second shell
		graph.addEdge(vB[2], vC[2]);
		graph.addEdge(vB[3], vD[3]);
		graph.addEdge(vB[5], vE[5]);
		
		// child 1
		graph.addEdge(vC[7], vD[7]);
		graph.addEdge(vC[8], vB[8]);
		
		// child 2
		graph.addEdge(vD[10], vE[10]);
		graph.addEdge(vD[11], vB[11]);
		
		// child 3
		graph.addEdge(vE[15], vB[15]);
		return graph;
	}
	
	private static ExecVertex[] genSeq(DirectedWeightedMultigraph<ExecVertex, ExecEdge> graph,
			Object owner, int num) {
		ExecVertex[] v = new ExecVertex[num];
		ExecVertex prev = null;
		for (int i = 0; i < num; i++) {
			v[i] = new ExecVertex(owner, i);
			graph.addVertex(v[i]);
			if (prev != null)
				graph.addEdge(prev, v[i]);
			prev = v[i];
		}
		return v;
	}

	private static DirectedWeightedMultigraph<ExecVertex, ExecEdge> makeGraph() {
		return new DirectedWeightedMultigraph<ExecVertex, ExecEdge>(new EdgeFactory<ExecVertex, ExecEdge>() {

			@Override
			public ExecEdge createEdge(ExecVertex a, ExecVertex b) {
				if (a.getTimestamp() > b.getTimestamp())
					throw new RuntimeException("Error: timstamps A is greater than timestamps b, time must always increase");
				return new ExecEdge(b.getTimestamp() - a.getTimestamp());
			}

		});
	}
	
	public static void main(String[] args) throws IOException {
		String base = "graph" + File.separator + "tests" + File.separator;
		GraphUtils.saveGraphDefault(makeBasic(), base + "basic");
		GraphUtils.saveGraphDefault(makeConcat(), base + "concat");
		GraphUtils.saveGraphDefault(makeEmbeded(), base + "embeded");
		GraphUtils.saveGraphDefault(makeInterleave(), base + "interleave");
		GraphUtils.saveGraphDefault(makeNested(), base + "nested");
		GraphUtils.saveGraphDefault(makeOpened1(), base + "open_1");
		GraphUtils.saveGraphDefault(makeOpened2(), base + "open_2");
		GraphUtils.saveGraphDefault(makeExecShell(), base + "shell");
	}
	
}
