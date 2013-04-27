package org.osm2world.core.map_elevation.creation;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osm2world.core.map_elevation.data.EleConnector;

/**
 * enforcer implementation that ignores many of the constraints,
 * but is much faster than the typical full implementation.
 * 
 * It tries to produce an output that is "good enough" for some purposes,
 * and is therefore a compromise between the {@link NoneEleConstraintEnforcer}
 * and a full implementation.
 */
public final class SimpleEleConstraintEnforcer implements EleConstraintEnforcer {
	
	private Collection<EleConnector> connectors = new ArrayList<EleConnector>();
	
	/**
	 * associates each EleConnector with the {@link StiffConnectorSet}
	 * it is part of (if any)
	 */
	private Map<EleConnector, StiffConnectorSet> stiffSetMap =
			new HashMap<EleConnector, StiffConnectorSet>();
	
	@Override
	public void addConnectors(Iterable<EleConnector> newConnectors) {
		
		for (EleConnector c : newConnectors) {
			connectors.add(c);
		}
		
		/* connect connectors */
		
		for (EleConnector c1 : newConnectors) {
			for (EleConnector c2 : connectors) {
				
				if (c1 != c2 && c1.connectsTo(c2)) {
					requireSameEle(c1, c2);
				}
				
			}
		}
		
	}
	
	@Override
	public void requireSameEle(EleConnector c1, EleConnector c2) {
		
		//SUGGEST (performance): a special case implementation would be faster
		
		requireSameEle(asList(c1, c2));
		
	}
	
	@Override
	public void requireSameEle(Iterable<EleConnector> cs) {
		
		/* find stiff sets containing any of the affected connectors */
		
		Set<EleConnector> looseConnectors = new HashSet<EleConnector>();
		Set<StiffConnectorSet> existingStiffSets = new HashSet<StiffConnectorSet>();
		
		for (EleConnector c : cs) {
			
			StiffConnectorSet stiffSet = stiffSetMap.get(c);
			
			if (stiffSet != null) {
				existingStiffSets.add(stiffSet);
			} else {
				looseConnectors.add(c);
			}
			
		}
		
		/* return if the connectors are already in a set together */
		
		if (existingStiffSets.size() == 1 && looseConnectors.isEmpty()) return;
		
		/* merge existing sets (if any) into a single set */
		
		StiffConnectorSet commonStiffSet = null;
		
		if (existingStiffSets.isEmpty()) {
			commonStiffSet = new StiffConnectorSet();
		} else {
			
			for (StiffConnectorSet stiffSet : existingStiffSets) {
				
				if (commonStiffSet == null) {
					commonStiffSet = stiffSet;
				} else {
					
					for (EleConnector c : stiffSet) {
						stiffSetMap.put(c, commonStiffSet);
					}
					
					commonStiffSet.mergeFrom(stiffSet);
					
				}
				
			}
			
		}
		
		/* add remaining (loose) connectors into the common set */
		
		for (EleConnector c : looseConnectors) {
			
			commonStiffSet.add(c);
			
			stiffSetMap.put(c, commonStiffSet);
			
		}
		
	}
	
	@Override
	public void requireVerticalDistance(ConstraintType type, double distance,
			EleConnector upper, EleConnector lower) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void requireIncline(ConstraintType type, double incline, List<EleConnector> cs) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void requireSmoothness(EleConnector from, EleConnector via, EleConnector to) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void enforceConstraints() {
		
		/* assign elevation to stiff sets by averaging terrain elevation */
		//TODO what for stiff sets above the ground?
		
		for (StiffConnectorSet stiffSet : stiffSetMap.values()) {
			
			double averageEle = 0;
			
			for (EleConnector connector : stiffSet) {
				averageEle += connector.getPosXYZ().y;
			}
			
			averageEle /= stiffSet.size();
			
			for (EleConnector connector : stiffSet) {
				connector.setPosXYZ(connector.pos.xyz(averageEle));
			}
			
		}
		
		/* TODO implement intended algorithm:
		 * - first assign ground ele to ON
		 * - then assign ele for ABOVE and BELOW based on min vertical distance constraints, and clearing
		 */
		
		for (EleConnector c : connectors) {
			
			//TODO use clearing
			
			switch (c.groundState) {
			case ABOVE: c.setPosXYZ(c.getPosXYZ().addY(5)); break;
			case BELOW: c.setPosXYZ(c.getPosXYZ().addY(-5)); break;
			default: //stay at ground elevation
			}
			
		}
		
	}
	
	/**
	 * a set of connectors that are required to have the same elevation
	 * TODO or a precise vertical offset
	 */
	private static class StiffConnectorSet implements Iterable<EleConnector> {
		
		//TODO maybe look for a more efficient set implementation
		private Set<EleConnector> connectors = new HashSet<EleConnector>();
		
		/**
		 * adds a connector to this set, requiring it to be at the set's
		 * reference elevation
		 */
		public void add(EleConnector connector) {
			connectors.add(connector);
		}
		
		/**
		 * combines this set with another, and makes the other set unusable.
		 * This set will contain all {@link EleConnector}s from the other set
		 * afterwards.
		 */
		public void mergeFrom(StiffConnectorSet otherSet) {
			
			connectors.addAll(otherSet.connectors);
			
			// make sure that the other set cannot be used anymore
			otherSet.connectors = null;
			
		}
		
		public double size() {
			return connectors.size();
		}
		
		@Override
		public Iterator<EleConnector> iterator() {
			return connectors.iterator();
		}
		
	}
	
}
