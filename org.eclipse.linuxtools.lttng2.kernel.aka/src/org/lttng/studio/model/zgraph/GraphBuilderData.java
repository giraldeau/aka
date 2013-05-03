package org.lttng.studio.model.zgraph;

import java.util.ArrayList;
import java.util.List;

public class GraphBuilderData {

	public Node head;
	public Node bounded;
	public Node unbounded;
	public int id = 0;
	public int len = 0;
	public int num = 0;
	public int depth = 0;
	public long delay = 0;
	public List<LinkType> types = new ArrayList<LinkType>();

}