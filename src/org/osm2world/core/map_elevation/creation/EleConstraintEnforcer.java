package org.osm2world.core.map_elevation.creation;

import java.util.List;

import org.osm2world.core.map_elevation.data.EleConnector;


public interface EleConstraintEnforcer {
	
	/**
	 * @param connectors  connector sets, with elevation values initially set to
	 *  terrain elevation at their xz position
	 */
	void addConnectors(Iterable<EleConnector> connectors);
	
	/**
	 * TODO javadoc
	 */
	public void addSameEleConstraint(EleConnector c1,
			EleConnector c2);
	
	/**
	 * TODO javadoc
	 */
	public void addSameEleConstraint(Iterable<EleConnector> cs);
	
	/**
	 * TODO javadoc
	 */
	void addMinVerticalDistanceConstraint(
			EleConnector upper, EleConnector lower, double distance);
	
	/**
	 * TODO javadoc
	 * @param cs
	 * @param minIncline
	 */
	void addMinInclineConstraint(List<EleConnector> cs, double minIncline);
	
	/**
	 * TODO javadoc
	 * @param cs
	 * @param maxIncline
	 */
	void addMaxInclineConstraint(List<EleConnector> cs, double maxIncline);
	
	/**
	 * TODO javadoc
	 */
	void addSmoothnessConstraint(EleConnector v2, EleConnector v1, EleConnector v3);
	
	/**
	 * tries to enforce the previously added constraints
	 * on elevations of connectors that have been added using
	 * {@link #addConnectors(Iterable)}
	 */
	void enforceConstraints();

}
