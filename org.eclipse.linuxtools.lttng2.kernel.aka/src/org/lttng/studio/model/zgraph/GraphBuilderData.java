package org.lttng.studio.model.zgraph;

import java.util.ArrayList;
import java.util.List;

public class GraphBuilderData {

	public Node head = new Node(0);
	public int len = 0;
	public int num = 0;
	public List<LinkType> types = new ArrayList<LinkType>();

}