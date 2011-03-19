package org.osm2world.core.osm.data;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;

public class OSMElement {

	public final TagGroup tags;
	public final long id;

	public OSMElement(TagGroup tags, long id) {
		assert tags != null;
		this.tags = tags;
		this.id = id;
	}
	
}
