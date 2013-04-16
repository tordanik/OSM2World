package org.osm2world.core.map_elevation.creation;

import java.util.List;

import org.osm2world.core.map_elevation.data.EleConnector;


public interface EleConstraintEnforcer {
	
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
	public void addSameEleConstraint(EleConnector c1,
			EleConnector c2);
	
	/**
	 * requires a number of connectors to be at the same elevation
	 */
	public void addSameEleConstraint(Iterable<EleConnector> cs);
	
	/**
	 * requires two connectors' elevations to differ at least by a given distance
	 */
	void addMinVerticalDistanceConstraint(
			EleConnector upper, EleConnector lower, double distance);
	
	/**
	 * requires a minimum incline along a sequence of connectors.
	 * 
	 * @param minIncline  incline value,
	 *  negative values are inclines in opposite direction
	 */
	void addMinInclineConstraint(List<EleConnector> cs, double minIncline);
	
	/**
	 * requires a maximum incline along a sequence of connectors
	 * 
	 * @param maxIncline  incline value,
	 *  negative values are inclines in opposite direction
	 */
	void addMaxInclineConstraint(List<EleConnector> cs, double maxIncline);
	
	/**
	 * makes sure that there is a "smooth" transition between two line segments
	 */
	void addSmoothnessConstraint(EleConnector c2, EleConnector c1, EleConnector c3);
	
	/**
	 * tries to enforce the previously added constraints
	 * on elevations of connectors that have been added using
	 * {@link #addConnectors(Iterable)}
	 */
	void enforceConstraints();

}
