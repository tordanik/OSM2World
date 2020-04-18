package org.osm2world.core.world.network;

import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.data.WorldObject;

/**
 * A linear component of a network.
 *
 * "Networks" are sets of {@link WorldObject}s that have certain
 * frequently required characteristics. Most importantly, a network
 * consists of nodes, lines and areas linked with each other.
 *
 * Features using these types of representation include roads,
 * railways and rivers.
 */
public interface NetworkWaySegmentWorldObject extends WaySegmentWorldObject {

	/**
	 * returns the line's width
	 */
	public float getWidth();

	/**
	 * Sets the calculated start of this network segment.
	 * This may be moved from the start of the OSM way to make room for features (junctions, crossings, ...) at nodes.
	 * To be used by {@link NetworkCalculator}.
	 */
	public void setStartCut(VectorXZ left, VectorXZ center, VectorXZ right);

	/**
	 * Sets the calculated end of this network segment.
	 * This may be moved from the start of the OSM way to make room for features (junctions, crossings, ...) at nodes.
	 * To be used by {@link NetworkCalculator}.
	 */
	public void setEndCut(VectorXZ left, VectorXZ center, VectorXZ right);

}
