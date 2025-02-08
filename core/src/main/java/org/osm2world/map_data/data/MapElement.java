package org.osm2world.map_data.data;

import java.util.Collection;
import java.util.List;

import org.osm2world.map_data.data.overlaps.MapOverlap;
import org.osm2world.math.BoundedObject;
import org.osm2world.world.data.WorldObject;

/**
 * An element from an OSM dataset.
 *
 * @see MapData
 */
public interface MapElement extends BoundedObject {

	/**
	 * returns the visual representations of this element.
	 *
	 * The order should match the order in which they were added,
	 * so that dependencies are preserved (elements that depend on
	 * another element should be placed after that element).
	 * The first element is considered the "primary" representation,
	 * and for some purposes - such as elevation calculation -, only this
	 * representation will be used.
	 */
	public List<? extends WorldObject> getRepresentations();

	/**
	 * returns the primary representation, or null if the object doesn't have any.
	 * @see #getRepresentations()
	 */
	public WorldObject getPrimaryRepresentation();

	/**
	 * returns all overlaps between this {@link MapElement}
	 * and other {@link MapElement}s.
	 */
	public Collection<MapOverlap<? extends MapElement, ? extends MapElement>> getOverlaps();

	/** returns this element's tags */
	TagSet getTags();

	/** returns the corresponding {@link MapRelationElement} */
	public MapRelationElement getElementWithId();

}
