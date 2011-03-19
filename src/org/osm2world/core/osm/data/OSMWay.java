package org.osm2world.core.osm.data;

import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;

public class OSMWay extends OSMElement {
	
	public final List<OSMNode> nodes;
	
	public OSMWay(TagGroup tags, long id, List<OSMNode> nodes) {
		super(tags, id);
		for (OSMNode node : nodes) assert node != null;
		this.nodes = nodes;
	}

	public boolean isClosed() {
		return nodes.size() > 0 && 
			nodes.get(0).equals(nodes.get(nodes.size()-1));
	}
	
	@Override
	public String toString() {
		
		if (OSMMember.useDebugLabels && tags.containsKey("debug:label")) {
			return tags.getValue("debug:label");
		}
		
		return id + ":" + nodes.get(0)
			+ "->[" + (nodes.size() - 2) + "]->"
			+ nodes.get(nodes.size()-1);
		
	}
	
}