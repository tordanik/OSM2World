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
		if (OSMMember.useDebugLabels && tags.containsKey("debug:label")) {
			return tags.getValue("debug:label");
		} else {
			return "(" + id + " at " + lat + ", " + lon + ", " + tags + ")";
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(lat);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(lon);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OSMNode other = (OSMNode) obj;
		if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
			return false;
		if (Double.doubleToLongBits(lon) != Double.doubleToLongBits(other.lon))
			return false;
		return true;
	}	
	
}