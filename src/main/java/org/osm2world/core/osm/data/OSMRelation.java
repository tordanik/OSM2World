package org.osm2world.core.osm.data;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;

public class OSMRelation extends OSMElement {

	public final List<OSMMember> relationMembers;
		// content added after constructor call

	public OSMRelation(TagGroup tags, long id, int initialMemberSize) {
		super(tags, id);
		this.relationMembers =
			new ArrayList<OSMMember>(initialMemberSize);
	}

	@Override
	public String toString() {
		return "r" + id;
	}

}