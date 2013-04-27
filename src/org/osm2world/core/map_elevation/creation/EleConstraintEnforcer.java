package org.osm2world.core.map_elevation.creation;

import java.util.List;

import org.osm2world.core.map_elevation.data.EleConnector;


public interface EleConstraintEnforcer {
	
	/** whether a constraint requires a minimum, maximum or exact value */
	public static enum ConstraintType {
		MIN, MAX, EXACT
	}
	
	/**
	 * makes connectors known to this enforcer. Only these connectors can be
	 * used in constraints later on, and only they will be affected by
	 * {@link #enforceConstraints()}.
	 * 
	 * @param connectors  connectors with elevation values initially set to
	 *  terrain elevation at their xz position
	 */
	void addConnectors(Iterable<EleConnector> connectors);
	
	/**
	 * requires two connectors to be at the same elevation
	 */
	public void requireSameEle(EleConnector c1, EleConnector c2);
	
	/**
	 * requires a number of connectors to be at the same elevation
	 */
	public void requireSameEle(Iterable<EleConnector> cs);
	
	/**
	 * requires two connectors' elevations to differ by a given distance
	 */
	void requireVerticalDistance(ConstraintType type, double distance,
			EleConnector upper, EleConnector lower);
	
	/**
	 * requires an incline along a sequence of connectors.
	 * 
	 * @param incline  incline value,
	 *  negative values are inclines in opposite direction
	 */
	void requireIncline(ConstraintType type, double incline,
			List<EleConnector> cs);
	
	/**
	 * requires that there is a "smooth" transition between two line segments
	 */
	void requireSmoothness(EleConnector from, EleConnector via, EleConnector to);
	
	/**
	 * tries to enforce the previously added constraints
	 * on elevations of connectors that have been added using
	 * {@link #addConnectors(Iterable)}
	 */
	void enforceConstraints();

}
