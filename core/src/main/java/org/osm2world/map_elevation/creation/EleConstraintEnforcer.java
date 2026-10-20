package org.osm2world.map_elevation.creation;

import java.util.List;

import org.osm2world.world.connectors.Connector;


public interface EleConstraintEnforcer {

	/** whether a constraint requires a minimum, maximum or exact value */
	public static enum ConstraintType {
		MIN, MAX, EXACT
	}

	/**
	 * makes connectors known to this enforcer. Only these connectors can be
	 * used in constraints later on.
	 *
	 * @param connectors  connectors with elevation values initially set to
	 *  terrain elevation at their xz position
	 */
	void addConnectors(Iterable<Connector> connectors);

	/**
	 * requires two connectors to be at the same elevation
	 */
	public void requireSameEle(Connector c1, Connector c2);

	/**
	 * requires a number of connectors to be at the same elevation
	 */
	public void requireSameEle(Iterable<Connector> cs);

	/**
	 * requires two connectors' elevations to differ by a given distance
	 */
	void requireVerticalDistance(ConstraintType type, double distance,
			Connector upper, Connector lower);

	/**
	 * requires a connector to be a give distance above a line segment
	 * defined by two other connectors.
	 *
	 * @param distance  distance, may be negative if 'upper' is actually below
	 */
	void requireVerticalDistance(ConstraintType type, double distance,
			Connector upper, Connector base1, Connector base2);

	/**
	 * requires an incline along a sequence of connectors.
	 *
	 * @param incline  incline value,
	 *  negative values are inclines in opposite direction
	 */
	void requireIncline(ConstraintType type, double incline,
			List<Connector> cs);

	/**
	 * requires that there is a "smooth" transition between two line segments
	 */
	void requireSmoothness(Connector from, Connector via, Connector to);

}
