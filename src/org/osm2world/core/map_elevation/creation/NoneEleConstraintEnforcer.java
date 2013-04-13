package org.osm2world.core.map_elevation.creation;

import java.util.List;

import org.osm2world.core.map_elevation.data.EleConnector;

/**
 * enforcer implementation that simply passes the interpolated terrain
 * elevations through, and does not actually enforce constraints.
 */
public class NoneEleConstraintEnforcer implements EleConstraintEnforcer {
	
	@Override
	public void addConnectors(Iterable<EleConnector> connectors) {}
	
	@Override
	public void addSameEleConstraint(EleConnector c1, EleConnector c2) {}
	
	@Override
	public void addSameEleConstraint(Iterable<EleConnector> cs) {}
	
	@Override
	public void addMinVerticalDistanceConstraint(
			EleConnector upper, EleConnector lower, double distance) {}
	
	@Override
	public void addMinInclineConstraint(
			List<EleConnector> cs, double minIncline) {}
	
	@Override
	public void addMaxInclineConstraint(
			List<EleConnector> cs, double maxIncline) {}
	
	@Override
	public void addSmoothnessConstraint(
			EleConnector c2, EleConnector c1, EleConnector c3) {}
	
	@Override
	public void enforceConstraints() {}
	
}
