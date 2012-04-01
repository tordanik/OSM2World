package org.osm2world.core.map_data.data;

import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_elevation.data.ElevationProfile;
import org.osm2world.core.math.datastructures.IntersectionTestObject;
import org.osm2world.core.osm.data.OSMElement;
import org.osm2world.core.world.data.WorldObject;

public interface MapElement extends IntersectionTestObject {
	
	public int getLayer();
	
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
	 * returns the elevation information for this GridElement.
	 * Can be null if no elevations have been set yet
	 * or if the element was ignored during elevation calculation
	 * because it didn't have a representation.
	 */
	public ElevationProfile getElevationProfile();
	
	/**
	 * returns all overlaps between this {@link MapElement}
	 * and other {@link MapElement}s.
	 */
	public Collection<MapOverlap<? extends MapElement, ? extends MapElement>> getOverlaps();

	/**
	 * returns the tags of the underlying {@link OSMElement}
	 */
	TagGroup getTags();
	
}
