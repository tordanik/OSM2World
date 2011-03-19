package org.osm2world.core.world.data;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXZ;

public interface WorldObject {

	/**
	 * returns whether this feature is on, above or below the ground.
	 * This is relevant for elevation calculations,
	 * because the elevation of features o.t.g. is directly
	 * determined by terrain elevation data.
	 * Elevation of features above/below t.g. depends on elevation of
	 * features o.t.g. as well as other features above/below t.g.
	 */
	public GroundState getGroundState();

	/**
	 * returns the amount of free space above the base elevation required by
	 * this feature. If pos isn't on this representation, the clearing
	 * for a somewhat nearby point on the feature should be returned.
	 */
	public double getClearingAbove(VectorXZ pos);

	/**
	 * returns the amount of free space below the base elevation required by
	 * this feature.  If pos isn't on this representation, the clearing
	 * for a somewhat nearby point on the feature should be returned.
	 */
	public double getClearingBelow(VectorXZ pos);
	
	/**
	 * returns the "primary" {@link MapElement} for this WorldObject;
	 * i.e. the one it is most strongly associated with.
	 * Can be null if there is no (clear) primary element for this feature.
	 */
	public MapElement getPrimaryMapElement();
	
}
