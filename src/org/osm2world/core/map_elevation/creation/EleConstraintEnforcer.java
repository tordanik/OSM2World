package org.osm2world.core.map_elevation.creation;

import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.JoinedEleConnectors;


public interface EleConstraintEnforcer {
	
	/**
	 * @param connectors  connector sets, with elevation values initially set to
	 *  terrain elevation at their xz position
	 */
	void addConnectors(Iterable<JoinedEleConnectors> connectors);
	
	/**
	 * TODO
	 */
	public void addSameEleConstraint(EleConnector c1,
			EleConnector c2);
	
	/**
	 * TODO
	 */
	public void addSameEleConstraint(Iterable<EleConnector> cs);
	
	/**
	 * tries to enforce the previously added constraints
	 * on elevations of connectors that have been added using
	 * {@link #addConnectors(Iterable)}
	 */
	void enforceConstraints();
	
}
