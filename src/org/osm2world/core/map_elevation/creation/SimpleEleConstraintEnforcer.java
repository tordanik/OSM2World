package org.osm2world.core.map_elevation.creation;

import java.util.List;

import org.osm2world.core.map_elevation.data.EleConnector;

/**
 * enforcer implementation that ignores many of the constraints,
 * but is much faster than the typical full implementation.
 * 
 * It tries to produce an output that is "good enough" for some purposes,
 * and is therefore a compromise between the {@link NoneEleConstraintEnforcer}
 * and a full implementation.
 * 
 * TODO implement
 */
public class SimpleEleConstraintEnforcer implements EleConstraintEnforcer {
	
	@Override
	public void addConnectors(Iterable<EleConnector> connectors) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addSameEleConstraint(EleConnector c1, EleConnector c2) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addSameEleConstraint(Iterable<EleConnector> cs) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addMinVerticalDistanceConstraint(EleConnector upper, EleConnector lower, double distance) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addMinInclineConstraint(List<EleConnector> cs, double minIncline) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addMaxInclineConstraint(List<EleConnector> cs, double maxIncline) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addSmoothnessConstraint(EleConnector c2, EleConnector c1, EleConnector c3) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void enforceConstraints() {
		// TODO Auto-generated method stub
		
	}
	
}
