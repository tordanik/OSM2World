package org.osm2world.map_data.data.overlaps;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapWaySegment;

/**
 * overlap between a {@link MapWaySegment} and a {@link MapArea} ("Way-Area").
 * The way either intersects with the area
 * or is completely contained within the area.
 */
public class MapOverlapNA extends MapOverlap<MapNode, MapArea> {

	public MapOverlapNA(MapNode node, MapArea area, MapOverlapType type) {
		super(node, area, type);
	}

}
