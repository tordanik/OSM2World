package org.osm2world.core.map_data.creation.index;

import org.osm2world.core.map_data.data.MapElement;

/**
 * index structure intended to speed up retrieval of candidates for
 * intersection and overlap tests
 */
public interface MapDataIndex {

	public abstract Iterable<? extends Iterable<MapElement>> getLeaves();
	
}
