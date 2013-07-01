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
	public void requireSameEle(EleConnector c1, EleConnector c2) {}
	
	@Override
	public void requireSameEle(Iterable<EleConnector> cs) {}
	
	@Override
	public void requireVerticalDistance(ConstraintType type, double distance,
			EleConnector upper, EleConnector lower) {}
	
	@Override
	public void requireVerticalDistance(ConstraintType type, double distance,
			EleConnector upper, EleConnector base1, EleConnector base2) {}
	
	@Override
	public void requireIncline(ConstraintType type, double incline,
			List<EleConnector> cs) {}
	
	@Override
	public void requireSmoothness(
			EleConnector from, EleConnector via, EleConnector to) {}
	
	@Override
	public void enforceConstraints() {}
	
}
