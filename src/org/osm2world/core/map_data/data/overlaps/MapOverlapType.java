package org.osm2world.core.map_data.data.overlaps;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapSegment;

//TODO: multiple ones at same time (areas that share segments AND intersect)!

public enum MapOverlapType {
	
	/** two {@link MapElement}s intersect */
	INTERSECT,
	
	/** one {@link MapElement} contains another {@link MapElement} */
	CONTAIN,
	
	/**
	 * {@link MapSegment#sharesBothNodes(MapSegment)}
	 * is true for a pair of {@link MapSegment}s, one from each {@link MapElement}
	 */
	SHARE_SEGMENT
	
}
