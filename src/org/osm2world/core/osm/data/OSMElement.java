package org.osm2world.core.osm.data;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;

public abstract class OSMElement {

	public final TagGroup tags;
	public final long id;

	public OSMElement(TagGroup tags, long id) {
		assert tags != null;
		this.tags = tags;
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
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
		OSMElement other = (OSMElement) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	/**
	 * returns the id, plus an one-letter prefix for the element type
	 */
	@Override
	public String toString() {
		return "?" + id;
	}
	
}
