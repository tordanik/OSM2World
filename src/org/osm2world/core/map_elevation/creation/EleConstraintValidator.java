package org.osm2world.core.map_elevation.creation;

import static java.lang.Math.abs;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.world.data.WorldObject;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * a wrapper for an {@link EleConstraintEnforcer} that passes all calls though,
 * but looks for obvious contradictions in the constraints to help with debugging.
 */
class EleConstraintValidator implements EleConstraintEnforcer {

	private final MapData mapData;
	private final EleConstraintEnforcer enforcer;
	
	private final Multimap<EleConnector, EleConnector> sameEleMap =
			HashMultimap.create();
	private final Multimap<EleConnector, EleConnector> verticalDistMap =
			HashMultimap.create();
	
	private final List<List<EleConnector>> smoothnessTriples =
			new ArrayList<List<EleConnector>>();
	
	public EleConstraintValidator(MapData mapData, EleConstraintEnforcer enforcer) {
		this.mapData = mapData;
		this.enforcer = enforcer;
	}
	
	@Override
	public void addConnectors(Iterable<EleConnector> connectors) {
		
		enforcer.addConnectors(connectors);
		
		for (EleConnector c : connectors) {
			sameEleMap.put(c, c);
		}
		
		for (EleConnector c1 : connectors) {
			for (EleConnector c2 : connectors) {
				
				if (c1 != c2 && c1.connectsTo(c2)) {
					sameEleMap.putAll(c1, sameEleMap.get(c2));
					sameEleMap.putAll(c2, sameEleMap.get(c1));
				}
				
			}
		}
		
	}

	@Override
	public void addSameEleConstraint(EleConnector c1, EleConnector c2) {
		
		enforcer.addSameEleConstraint(c1, c2);

		if (verticalDistMap.containsEntry(c1, c2)) {
			failValidation("vertical distance despite same ele", c1, c2);
		}

		sameEleMap.putAll(c1, sameEleMap.get(c2));
		sameEleMap.putAll(c2, sameEleMap.get(c1));
		
	}

	@Override
	public void addSameEleConstraint(Iterable<EleConnector> cs) {
		
		enforcer.addSameEleConstraint(cs);
		
		for (EleConnector c1 : cs) {
			for (EleConnector c2 : cs) {
				
				if (verticalDistMap.containsEntry(c1, c2)) {
					failValidation("vertical distance despite same ele", c1, c2);
				}
				
				sameEleMap.putAll(c1, sameEleMap.get(c2));
				sameEleMap.putAll(c2, sameEleMap.get(c1));
							
			}
		}
		
	}

	@Override
	public void addMinVerticalDistanceConstraint(EleConnector upper, EleConnector lower, double distance) {
		
		enforcer.addMinVerticalDistanceConstraint(upper, lower, distance);
		
		if (sameEleMap.containsEntry(upper, lower)) {
			failValidation("vertical distance despite same ele", upper, lower);
		}

		verticalDistMap.putAll(upper, sameEleMap.get(lower));
		verticalDistMap.putAll(lower, sameEleMap.get(upper));
		
	}

	@Override
	public void addMinInclineConstraint(List<EleConnector> cs, double minIncline) {
		enforcer.addMinInclineConstraint(cs, minIncline);
	}

	@Override
	public void addMaxInclineConstraint(List<EleConnector> cs, double maxIncline) {
		enforcer.addMaxInclineConstraint(cs, maxIncline);
	}

	@Override
	public void addSmoothnessConstraint(EleConnector v2, EleConnector v1, EleConnector v3) {
		
		enforcer.addSmoothnessConstraint(v2, v1, v3);
		
		smoothnessTriples.add(asList(v1, v2, v3));
		
	}

	@Override
	public void enforceConstraints() {
		
		enforcer.enforceConstraints();
		
		printSmoothnessLog();
		
	}
	
	private void printSmoothnessLog() {
		
		StringBuilder log = new StringBuilder("smoothness log:\n");
		
		for (List<EleConnector> triple : smoothnessTriples) {
			
			double inc1 = getIncline(
					triple.get(1).getPosXYZ(),
					triple.get(0).getPosXYZ());
			
			double inc2 = getIncline(
					triple.get(2).getPosXYZ(),
					triple.get(1).getPosXYZ());
			
			double inclineDiff = abs(inc2 - inc1);
			double dist = triple.get(0).pos.distanceTo(triple.get(2).pos);
			double inclineDiffPerMeter = inclineDiff / dist;
			
			if (inclineDiffPerMeter > 200) {
				log.append(String.format("%.1f%% over %.1fm at ", inclineDiff * 100,
						dist));
				appendEleConnectorString(log, triple.get(1));
				log.append('\n');
			}
			
		}
		
		System.out.println(log);
		
	}

	private double getIncline(VectorXYZ v1, VectorXYZ v2) {
		return (v2.y - v1.y) / v1.distanceToXZ(v2);
	}

	private void failValidation(String constraintText, EleConnector... cs ) {
		
		StringBuilder text = new StringBuilder("invalid constraint:\n");
		text.append(constraintText);
		
		text.append("participating connectors:\n");
		
		for (EleConnector c : cs) {
			appendEleConnectorString(text, c);
			text.append('\n');
		}
		
		throw new Error(text.toString());
		
	}

	private void appendEleConnectorString(StringBuilder out, EleConnector c) {
		
		out.append(c);
		
		for (WorldObject wo : mapData.getWorldObjects()) {
			for (EleConnector woConnector : wo.getEleConnectors()) {
				if (woConnector == c) {
					out.append(" from " + wo);
				}
			}
		}
		
	}
	
}
