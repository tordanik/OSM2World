package org.osm2world.core.osm.data;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;

public class OSMNode extends OSMElement {
	public final double lat;
	public final double lon;
	
	public OSMNode(double lat, double lon, TagGroup tags, long id) {
		super(tags, id);
		this.lat = lat;
		this.lon = lon;
	}
	
	@Override
	public String toString() {
		return "n" + id;
	}
	
}