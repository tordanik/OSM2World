package org.osm2world.core.map_data.creation;

import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.osm.data.OSMElement;
import org.osm2world.core.osm.data.OSMMember;
import org.osm2world.core.osm.data.OSMNode;
import org.osm2world.core.osm.data.OSMRelation;
import org.osm2world.core.osm.data.OSMWay;

/**
 * utility class for creating areas from multipolygon relations,
 * including those with non-closed member ways.
 * 
 * Known Limitations:<ul>
 * <li>This cannot reliably handle touching inner rings consisting
 * of non-closed ways.</li>
 * <li>Closed touching rings will not break the calculation,
 * but are represented as multiple touching holes.</li>
 * </ul>
 */
final class MultipolygonAreaBuilder {
	
	/** prevents instantiation */
	private MultipolygonAreaBuilder() { }
	
	/**
	 * Creates areas for a multipolygon relation.
	 * Also adds this area to the adjacent nodes using
	 * {@link MapNode#addAdjacentArea(MapArea)}.
	 * 
	 * @param relation  the multipolygon relation
	 * @param nodeMap   map from {@link OSMNode}s to {@link MapNode}s
	 * 
	 * @return  constructed area(s), multiple areas will be created if there
	 *          is more than one outer ring. Empty for invalid multipolygons.
	 */
	public static final Collection<MapArea> createAreasForMultipolygon(
			OSMRelation relation, Map<OSMNode, MapNode> nodeMap) {
		
		if (isSimpleMultipolygon(relation)) {
			return createAreasForSimpleMultipolygon(relation, nodeMap);
		} else {
			return createAreasForAdvancedMultipolygon(relation, nodeMap);
		}
		
	}

	private static final boolean isSimpleMultipolygon(OSMRelation relation) {
		
		int numberOuters = 0;
		boolean closedWays = true;
		
		for (OSMMember member : relation.relationMembers) {
			
			if ("outer".equals(member.role)) {
				numberOuters += 1;
			}
			
			if (("outer".equals(member.role) || "inner".equals(member.role))
					&& member.member instanceof OSMWay) {
				
				if (!((OSMWay)member.member).isClosed()) {
					closedWays = false;
					break;
				}
				
			}
			
		}
		
		return numberOuters == 1 && closedWays;
		
	}

	/**
	 * handles the common simple case with only one outer way.
	 * Expected to be faster than the more general method
	 * {@link #createAreasForAdvancedMultipolygon(OSMRelation, Map)}
	 * 
	 * @param relation  has to be a simple multipolygon relation
	 * @param nodeMap
	 */
	private static final Collection<MapArea> createAreasForSimpleMultipolygon(
			OSMRelation relation, Map<OSMNode, MapNode> nodeMap) {
		
		assert isSimpleMultipolygon(relation);
		
		OSMElement tagSource = null;
		List<MapNode> outerNodes = null;
		List<List<MapNode>> holes = new ArrayList<List<MapNode>>();
		
		for (OSMMember member : relation.relationMembers) {
			if (member.member instanceof OSMWay) {
				
				OSMWay way = (OSMWay)member.member;
				
				if ("inner".equals(member.role)) {
					
					List<MapNode> hole = new ArrayList<MapNode>(way.nodes.size());
					
					for (OSMNode node : ((OSMWay)member.member).nodes) {
						hole.add(nodeMap.get(node));
						//TODO: add area as adjacent to node for inners' nodes, too?
					}
					
					holes.add(hole);

				} else if ("outer".equals(member.role)) {
					
					tagSource = relation.tags.size() > 1 ? relation : way;
					
					outerNodes = new ArrayList<MapNode>(way.nodes.size());
					for (OSMNode node : way.nodes) {
						outerNodes.add(nodeMap.get(node));
					}
					
				}
				
			}
		}
		
		return singleton(new MapArea(tagSource, outerNodes, holes));
		
	}

	private static final Collection<MapArea> createAreasForAdvancedMultipolygon(
			OSMRelation relation, Map<OSMNode, MapNode> nodeMap) {
		
		ArrayList<Ring> rings = buildRings(relation, nodeMap);
		
		if (rings != null) {
			
			return buildPolygonsFromRings(relation, rings);
			
		}
		
		return Collections.emptyList();
		
	}

	private static final ArrayList<Ring> buildRings(
			OSMRelation relation, Map<OSMNode, MapNode> nodeMap) {
		
		ArrayList<Ring> rings = new ArrayList<Ring>();
		ArrayList<OSMWay> unassignedWays = new ArrayList<OSMWay>();
		
		/* collect ways */
		
		for (OSMMember member : relation.relationMembers) {
			if (member.member instanceof OSMWay
					&& ("outer".equals(member.role)
							|| "inner".equals(member.role)) ) {
				
				unassignedWays.add((OSMWay)member.member);
				
			}
		}
		
		/* build rings */
		
		Ring currentRing = null;
		
		while (unassignedWays.size() > 0) {
		
			if (currentRing == null) {
				
				// start a new ring with any unassigned way
				
				OSMWay unassignedWay = unassignedWays.remove(unassignedWays.size() - 1);
				currentRing = new Ring(unassignedWay);
								
			} else {
				
				// try to continue the ring by appending a way
				
				OSMWay nextWay = null;
				
				for (OSMWay unassignedWay : unassignedWays) {
				
					if (currentRing.wayContinuesRing(unassignedWay)) {
						nextWay = unassignedWay;
						break;
					}
					
				}
				
				if (nextWay != null) {
					unassignedWays.remove(nextWay);
					currentRing.add(nextWay);
				} else {
					return null;
				}
				
			}
			
			// check whether the ring is closed
			
			if (currentRing.isClosed()) {
				currentRing.calculateFieldValues(nodeMap);
				rings.add(currentRing);
				currentRing = null;
			}
		
		}
		
		if (currentRing != null) {
			// the last ring could not be completed
			return null;
		} else {
			return rings;
		}
		
	}

	/**
	 * @param rings  rings to build polygons from; will be empty afterwards
	 */
	private static final Collection<MapArea> buildPolygonsFromRings(
			OSMRelation relation, List<Ring> rings) {
		
		Collection<MapArea> finishedPolygons = new ArrayList<MapArea>(rings.size() / 2);
				
		/* build polygon */
		
		while (!rings.isEmpty()) {
			
			/* find an outer ring */
			
			Ring outerRing = null;
			
			for (Ring candidate : rings) {
				
				boolean containedInOtherRings = false;
				
				for (Ring otherRing : rings) {
					if (otherRing != candidate
							&& otherRing.contains(candidate)) {
						containedInOtherRings = true;
						break;
					}
				}
				
				if (!containedInOtherRings) {
					outerRing = candidate;
					break;
				}
				
			}
			
			/* find inner rings of that ring */
			
			Collection<Ring> innerRings = new ArrayList<Ring>();
			
			for (Ring ring : rings) {
				if (ring != outerRing && outerRing.contains(ring)) {
					
					boolean containedInOtherRings = false;
					
					for (Ring otherRing : rings) {
						if (otherRing != ring && otherRing != outerRing
								&& otherRing.contains(ring)) {
							containedInOtherRings = true;
							break;
						}
					}
					
					if (!containedInOtherRings) {
						innerRings.add(ring);
					}
					
				}
			}
			
			/* create a new area and remove the used rings */
			
			List<List<MapNode>> holes = new ArrayList<List<MapNode>>(innerRings.size());
			List<SimplePolygonXZ> holesXZ = new ArrayList<SimplePolygonXZ>(innerRings.size());
			
			for (Ring innerRing : innerRings) {
				holes.add(innerRing.mapNodeLoop);
				holesXZ.add(innerRing.polygon);
			}
			
			MapArea area = new MapArea(relation, outerRing.mapNodeLoop, holes,
					new PolygonWithHolesXZ(outerRing.polygon, holesXZ));
			
			finishedPolygons.add(area);
			
			rings.remove(outerRing);
			rings.removeAll(innerRings);
			
		}
		
		return finishedPolygons;
		
	}

	private static final class Ring {
		
		private List<OSMWay> ways = new ArrayList<OSMWay>();
		
		public Ring(OSMWay firstWay) {
			super();
			ways.add(firstWay);
		}
		
		public void add(OSMWay way) {
			ways.add(way);
		}

		public boolean isClosed() {
			
			OSMNode firstNode = ways.get(0).nodes.get(0);
			
			List<OSMNode> nodesLastWay = ways.get(ways.size()-1).nodes;
			OSMNode lastNode = nodesLastWay.get(nodesLastWay.size()-1);
			
			return firstNode == lastNode;
			
		}

		public boolean wayContinuesRing(OSMWay way) {
			
			List<OSMNode> nodesLastWay = ways.get(ways.size()-1).nodes;
			OSMNode lastNode = nodesLastWay.get(nodesLastWay.size()-1);
			
			return way.nodes.get(0) == lastNode;
			
		}

		public List<MapNode> mapNodeLoop = null;
		public SimplePolygonXZ polygon = null;
		
		public void calculateFieldValues(Map<OSMNode, MapNode> nodeMap) {
			
			assert polygon == null;
			assert isClosed();
			
			mapNodeLoop = new ArrayList<MapNode>();
			
			for (OSMWay way : ways) {
				for (int i = 0; i < way.nodes.size(); i++) {
					
					OSMNode node = way.nodes.get(i);
					
					if (i == 0 && !mapNodeLoop.isEmpty()) {
						//node has already been added
						assert nodeMap.get(node) ==
								mapNodeLoop.get(mapNodeLoop.size() - 1);
					} else {
						mapNodeLoop.add(nodeMap.get(node));
					}
					
				}
			}
			
			polygon = MapArea.polygonFromMapNodeLoop(mapNodeLoop);
			
		}
		
		public boolean contains(Ring other) {
			return this.polygon.contains(other.polygon);
		}
		
	}
	
}
