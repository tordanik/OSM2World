package org.osm2world.map_data.data.overlaps;

import java.util.Collection;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapElement;
import org.osm2world.math.VectorXZ;

/**
 * overlap between two areas ("Area-Area").
 * The two areas' outlines either intersect
 * or one of them is completely within the other one.
 */
public class MapOverlapAA extends MapOverlap<MapArea, MapArea> {

	public MapOverlapAA(MapArea area1, MapArea area2, MapOverlapType type) {
		super(area1, area2, type);
	}

	@Override
	public MapArea getOther(MapElement element) {
		return (MapArea) super.getOther(element);
	}

	public Collection<VectorXZ> getIntersectionPositions() {
		return e1.getPolygon().intersectionPositions(e2.getPolygon());
	}

}
