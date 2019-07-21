package org.osm2world.core.world.network;

import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.world.creation.NetworkCalculator;
import org.osm2world.core.world.data.WorldObject;

/**
 * "networks" are sets of {@link WorldObject}s that have certain
 * frequently required characteristics. Most importantly, a network
 * consists of nodes, lines and areas linked with each other.
 *
 * Other characteristics include
 * - cut angles where lines (or lines and areas) connect
 * - junctions at nodes that occupy some area and push back connecting lines
 *  //TODO (documentation): explain more
 *
 * Features using these types of representation include roads,
 * railways and rivers.
 */
public interface NetworkWaySegmentWorldObject extends WorldObject {

	/**
	 * returns the line's width
	 */
	public float getWidth();

	/**
	 * returns the cut vector for the start of the line.
	 * Only available after {@link #setStartCutVector(VectorXZ)}.
	 */
	public VectorXZ getStartCutVector();

	/**
	 * Sets the cut vector for the start of the line. //TODO: explain "cut vectors"
	 * To be used by {@link NetworkCalculator}.
	 */
	public void setStartCutVector(VectorXZ cutVector);

	/**
	 * returns the cut vector for the end of the line.
	 * Only available after {@link #setStartCutVector(VectorXZ)}.
	 */
	public VectorXZ getEndCutVector();

	/**
	 * Sets the cut vector for the end of the line. //TODO: explain "cut vectors"
	 * To be used by {@link NetworkCalculator}.
	 */
	public void setEndCutVector(VectorXZ cutVector);

	/**
	 * returns the current offset for the end of the line.
	 * Should already be usable before first {@link #setEndOffset(VectorXZ)}
	 * call, returning (0,0).
	 */
	public VectorXZ getStartOffset();

	/**
	 * Sets the offset for the start of the line.
	 * To be used by {@link NetworkCalculator}.
	 */
	public void setStartOffset(VectorXZ offsetVector);

	/**
	 * returns the current offset for the end of the line.
	 * Should already be usable before first {@link #setEndOffset(VectorXZ)}
	 * call, returning (0,0).
	 */
	public VectorXZ getEndOffset();

	/**
	 * Sets the offset for the end of the line.
	 * To be used by {@link NetworkCalculator}.
	 */
	public void setEndOffset(VectorXZ offsetVector);

}
