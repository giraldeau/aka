package org.lttng.studio.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jgrapht.Graph;
import org.jgrapht.ext.DOTExporter;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecVertex;

public class GraphUtils {

	public static void saveGraphDefault(Graph<ExecVertex, ExecEdge> graph, String path) throws IOException {
		saveGraph(graph, new File(path + ".dot").toString());
	}

	public static void saveGraph(Graph<ExecVertex, ExecEdge> graph, String path) throws IOException {
		DOTExporter<ExecVertex, ExecEdge> dot = ExecGraphProviders.getDOTExporter();
		FileWriter fwriter = new FileWriter(new File(path));
		dot.export(fwriter, graph);
		fwriter.flush();
	}

}
