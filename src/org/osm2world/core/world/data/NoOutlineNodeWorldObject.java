package org.osm2world.core.world.data;

import static java.util.Collections.singleton;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.datastructures.IntersectionTestObject;

/**
 * superclass for {@link NodeWorldObject}s that don't have an outline,
 * and are not part of a network.
 * Instead, they are located at a single point on the terrain or other areas
 * and not connected to other features.
 */
public abstract class NoOutlineNodeWorldObject implements NodeWorldObject,
		IntersectionTestObject {
	
	protected final MapNode node;
	
	public NoOutlineNodeWorldObject(MapNode node) {
		this.node = node;
	}
	
	@Override
	public final MapElement getPrimaryMapElement() {
		return node;
	}
	
	@Override
	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
		return new AxisAlignedBoundingBoxXZ(singleton(node.getPos()));
	}
	
}
