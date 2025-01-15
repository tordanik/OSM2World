package org.osm2world.core.map_data.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * a multipolygon relation containing multiple outer rings.
 * Unlike simpler multipolygons, this cannot be represented as a single {@link MapArea}.
 * Contains several {@link MapArea} elements as members.
 */
public class MapMultipolygonRelation extends MapRelation {

	private final List<MapArea> areas = new ArrayList<>();

	public MapMultipolygonRelation(long id, TagSet tags, Collection<MapArea> areas) {

		super(id, tags);

		if (!tags.contains("type", "multipolygon")) {
			throw new IllegalArgumentException("not a multipolygon relation");
		}

		areas.forEach(it -> this.addMember("outer", it));

	}

	@Override
	public void addMember(String role, MapRelationElement element) {
		if (role.equals("outer") && element instanceof MapArea a) {
			super.addMember(role, element);
			areas.add(a);
		} else {
			throw new IllegalArgumentException("invalid role or member type");
		}
	}

	public List<MapArea> getAreas() {
		return areas;
	}

}
