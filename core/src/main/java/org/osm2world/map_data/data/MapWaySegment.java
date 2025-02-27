package org.osm2world.map_data.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osm2world.map_data.data.overlaps.MapIntersectionWW;
import org.osm2world.map_data.data.overlaps.MapOverlap;
import org.osm2world.world.data.WaySegmentWorldObject;

import com.google.common.collect.Iterables;


/**
 * Segment (the straight line between two successive nodes) of a way from an OSM dataset.
 *
 * @see MapData
 */
public class MapWaySegment extends MapSegment implements MapElement {

	private final List<WaySegmentWorldObject> representations = new ArrayList<WaySegmentWorldObject>(1);

	private final MapWay way;

	@SuppressWarnings("unchecked") //is later checked for EMPTY_LIST using ==
	private Collection<MapOverlap<?,?>> overlaps = Collections.EMPTY_LIST;

	MapWaySegment(MapWay way, MapNode startNode, MapNode endNode) {
		super(startNode, endNode);
		this.way = way;
	}

	/** returns this segment's parent {@link MapWay} */
	public MapWay getWay() {
		return way;
	}

	/** returns the parent {@link MapWay}'s tags */
	@Override
	public TagSet getTags() {
		return getWay().getTags();
	}

	@Override
	public MapRelationElement getElementWithId() {
		return getWay();
	}

	public void addOverlap(MapOverlap<?, ?> overlap) {
		assert overlap.e1 == this || overlap.e2 == this;
		if (overlaps == Collections.EMPTY_LIST) {
			overlaps = new ArrayList<MapOverlap<?,?>>();
		}
		overlaps.add(overlap);
	}

	@Override
	public Collection<MapOverlap<?,?>> getOverlaps() {
		return overlaps;
	}

	public Iterable<MapIntersectionWW> getIntersectionsWW() {
		return Iterables.filter(overlaps, MapIntersectionWW.class);
	}

	@Override
	public List<WaySegmentWorldObject> getRepresentations() {
		return representations;
	}

	@Override
	public WaySegmentWorldObject getPrimaryRepresentation() {
		if (representations.isEmpty()) {
			return null;
		} else {
			return representations.get(0);
		}
	}

	/**
	 * adds a visual representation for this way segment
	 */
	public void addRepresentation(WaySegmentWorldObject representation) {
		this.representations.add(representation);
	}

	@Override
	public String toString() {
		return way + "[" + way.getWaySegments().indexOf(this) + "]";
	}

}
