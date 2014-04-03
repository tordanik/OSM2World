package org.osm2world.core.map_data.creation.index;

import java.util.Collection;

import org.osm2world.core.map_data.data.MapElement;

/**
 * index structure intended to speed up retrieval of candidates for
 * intersection and overlap tests
 */
public interface MapDataIndex {
	
	/**
	 * inserts the element into the index structure
	 */
	public void insert(MapElement e);
	
	/**
	 * inserts the element into the index structure,
	 * and returns all nearby elements contained the index structure
	 * 
	 * @return leaves the element ends up in. A subset of {@link #getLeaves()}.
	 * The leaves already contain the new element.
	 */
	public Collection<? extends Iterable<MapElement>> insertAndProbe(MapElement e);
	
	/**
	 * returns all leaves of this index structure
	 * 
	 * @return duplicate-free groups of elements
	 */
	public abstract Iterable<? extends Iterable<MapElement>> getLeaves();
	
}
