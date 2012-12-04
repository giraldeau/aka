package org.lttng.studio.utils;

import java.util.HashMap;
import java.util.Map;

import org.jgrapht.ext.ComponentAttributeProvider;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.lttng.studio.model.graph.ExecEdge;
import org.lttng.studio.model.graph.ExecVertex;
import org.lttng.studio.model.kernel.Task;

/*
 * This class is ugly. Should look to http://code.google.com/p/google-guice/
 * */

public class ExecGraphProviders {

	static VertexNameProvider<ExecVertex> vertexNameProvider;
	static VertexNameProvider<ExecVertex> vertexIDProvider;
	static EdgeNameProvider<ExecEdge> edgeNameProvider;
	static ComponentAttributeProvider<ExecVertex> vertexAttributeProvider;
	static ComponentAttributeProvider<ExecEdge> edgeAttributeProvider;

	public static VertexNameProvider<ExecVertex> getVertexNameProvider() {
		if (vertexNameProvider == null) {
			vertexNameProvider = new VertexNameProvider<ExecVertex>() {
				@Override
				public String getVertexName(ExecVertex vertex) {
					if (vertex.getOwner() instanceof Task) {
						Task task = (Task) vertex.getOwner();
						return String.format("[%d] %d", vertex.getId(), task.getTid());
					}
					return String.format("[%d]", vertex.getId());
				}
			};
		}
		return vertexNameProvider;
	}

	public static VertexNameProvider<ExecVertex> getVertexIDProvider() {
		if (vertexIDProvider == null) {
			vertexIDProvider = new VertexNameProvider<ExecVertex>() {
				@Override
				public String getVertexName(ExecVertex vertex) {
					return String.format("%d", vertex.getId());
				}
			};
		}
		return vertexIDProvider;
	}

	public static EdgeNameProvider<ExecEdge> getEdgeNameProvider() {
		if (edgeNameProvider == null) {
			edgeNameProvider = new EdgeNameProvider<ExecEdge>() {
				@Override
				public String getEdgeName(ExecEdge edge) {
					return String.format("%s", edge.getType());
				}
			};
		}
		return edgeNameProvider;
	}

	public static ComponentAttributeProvider<ExecVertex> getVertexAttributeProvider() {
		if (vertexAttributeProvider == null) {
			vertexAttributeProvider = new ComponentAttributeProvider<ExecVertex>() {
				@Override
				public Map<String, String> getComponentAttributes(ExecVertex vertex) {
					return new HashMap<String, String>();
				}
			};
		}
		return vertexAttributeProvider;
	}

	public static ComponentAttributeProvider<ExecEdge> getEdgeAttributeProvider() {
		if (edgeAttributeProvider == null) {
			edgeAttributeProvider = new ComponentAttributeProvider<ExecEdge>() {
				@Override
				public Map<String, String> getComponentAttributes(ExecEdge edge) {
					HashMap<String, String> attr = new HashMap<String, String>();
					return attr;
				}
			};
		}
		return edgeAttributeProvider;
	}

	public static DOTExporter<ExecVertex, ExecEdge> getDOTExporter() {
		return new DOTExporter<ExecVertex, ExecEdge>(ExecGraphProviders.getVertexIDProvider(),
					ExecGraphProviders.getVertexNameProvider(), ExecGraphProviders.getEdgeNameProvider(),
					ExecGraphProviders.getVertexAttributeProvider(), ExecGraphProviders.getEdgeAttributeProvider());
	}
}
