package org.osm2world.core.map_data.data.overlaps;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapWaySegment;

/**
 * supertype for intersections and inclusions
 * between two {@link MapElement}s
 */
public abstract class MapOverlap<T1 extends MapElement, T2 extends MapElement> {
	
	public final T1 e1;
	public final T2 e2;
	
	public final MapOverlapType type;
	
	public MapOverlap(T1 e1, T2 e2, MapOverlapType type) {
		this.e1 = e1;
		this.e2 = e2;
		this.type = type;
	}

	/**
	 * takes one of the {@link MapWaySegment}s that participate
	 * in this overlap and returns the other one
	 */
	public MapElement getOther(MapElement element) {
		if (element == e1) {
			return e2;
		} else if (element == e2) {
			return e1;
		} else {
			throw new IllegalArgumentException("element isn't part of this intersection");
		}
	}
	
	@Override
	public String toString() {
		return "( " + e1.toString() + " - " + e2.toString() + " )";
	}
	
}
