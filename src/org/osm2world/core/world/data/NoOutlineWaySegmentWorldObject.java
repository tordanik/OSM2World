package org.osm2world.core.world.data;

import java.util.Arrays;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.datastructures.IntersectionTestObject;

/**
 * superclass for {@link WaySegmentWorldObject}s that don't have an outline,
 * and are not part of a network.
 * Instead, they can be considered an infinitely thin.
 */
public abstract class NoOutlineWaySegmentWorldObject
		implements WaySegmentWorldObject, IntersectionTestObject {
	
	final MapWaySegment segment;
	
	public NoOutlineWaySegmentWorldObject(MapWaySegment segment) {
		this.segment = segment;
	}
	
	@Override
	public MapElement getPrimaryMapElement() {
		return segment;
	}
	
	@Override
	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
		return new AxisAlignedBoundingBoxXZ(Arrays.asList(
				segment.getStartNode().getPos(),
				segment.getEndNode().getPos()));
	}
	
}
