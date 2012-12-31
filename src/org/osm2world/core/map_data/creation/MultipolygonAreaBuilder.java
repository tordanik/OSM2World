package org.osm2world.core.map_data.creation;

import static java.lang.Boolean.*;
import static java.lang.Double.NaN;
import static java.lang.Math.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.osm2world.core.math.GeometryUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.plugins.graphview.core.data.MapBasedTagGroup;
import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IntersectionTestObject;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.osm.data.OSMElement;
import org.osm2world.core.osm.data.OSMMember;
import org.osm2world.core.osm.data.OSMNode;
import org.osm2world.core.osm.data.OSMRelation;
import org.osm2world.core.osm.data.OSMWay;
import org.osm2world.core.osm.ruleset.HardcodedRuleset;
import org.osm2world.core.osm.ruleset.Ruleset;

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
			
			if ("outer".equals(member.role)
					&& member.member instanceof OSMWay) {
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
		
		List<NodeSequence> innersAndOuters = new ArrayList<NodeSequence>();
		
		/* collect ways */
		
		for (OSMMember member : relation.relationMembers) {
			if (member.member instanceof OSMWay
					&& ("outer".equals(member.role)
							|| "inner".equals(member.role)) ) {
				
				innersAndOuters.add(new NodeSequence(
						(OSMWay)member.member, nodeMap));
				
			}
		}
		
		/* build rings, then polygons from the ways */
		
		List<Ring> rings = buildRings(innersAndOuters);
		
		if (rings != null) {
						
			return buildPolygonsFromRings(relation, rings);
			
		} else {
		
			return Collections.emptyList();
			
		}
		
	}

	/**
	 * builds closed rings from any mixture of closed and unclosed segments
	 * 
	 * @return  null if building closed rings isn't possible
	 */
	private static final List<Ring> buildRings(
			List<NodeSequence> sequences) {
		
		List<Ring> closedRings = new ArrayList<Ring>();
		
		NodeSequence currentRing = null;
		
		while (sequences.size() > 0) {
			
			if (currentRing == null) {
				
				// start a new ring with any remaining node sequence
				
				currentRing = sequences.remove(sequences.size() - 1);
				
			} else {
				
				// try to continue the ring by appending a node sequence
				
				NodeSequence assignedSequence = null;
				
				for (NodeSequence sequence : sequences) {
				
					if (currentRing.tryAdd(sequence)) {
						assignedSequence = sequence;
						break;
					}
					
				}
				
				if (assignedSequence != null) {
					sequences.remove(assignedSequence);
				} else {
					return null;
				}
				
			}
			
			// check whether the ring under construction is closed
			
			if (currentRing != null && currentRing.isClosed()) {
				closedRings.add(new Ring(currentRing));
				currentRing = null;
			}
		
		}
		
		if (currentRing != null) {
			// the last ring could not be closed
			return null;
		}
		
		return closedRings;
		
	}

	/**
	 * @param rings  rings to build polygons from; will be empty afterwards
	 */
	private static final Collection<MapArea> buildPolygonsFromRings(
			OSMRelation relation, List<Ring> rings) {
		
		Collection<MapArea> finishedPolygons =
				new ArrayList<MapArea>(rings.size() / 2);
		
		/* build polygon */
		
		while (!rings.isEmpty()) {
			
			/* find an outer ring */
			
			Ring outerRing = null;
			
			for (Ring candidate : rings) {
				
				boolean containedInOtherRings = false;
				
				for (Ring otherRing : rings) {
					if (otherRing != candidate
							&& otherRing.containsRing(candidate)) {
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
				if (ring != outerRing && outerRing.containsRing(ring)) {
					
					boolean containedInOtherRings = false;
					
					for (Ring otherRing : rings) {
						if (otherRing != ring && otherRing != outerRing
								&& otherRing.containsRing(ring)) {
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
				holes.add(innerRing.closedNodeSequence);
				holesXZ.add(innerRing.getPolygon());
			}
			
			MapArea area = new MapArea(relation, outerRing.getNodeLoop(), holes,
					new PolygonWithHolesXZ(outerRing.getPolygon(), holesXZ));
			
			finishedPolygons.add(area);
			
			rings.remove(outerRing);
			rings.removeAll(innerRings);
			
		}
		
		return finishedPolygons;
		
	}
	
	private static final TagGroup COASTLINE_NODE_TAGS = new MapBasedTagGroup(
			new Tag("osm2world:note", "fake node from coastline processing"));
	
	/**
	 * turns all coastline ways into {@link MapArea}s
	 * based on an artificial natural=water multipolygon relation.
	 * 
	 * It relies on the direction-dependent drawing of coastlines.
	 * If coastlines are incomplete, then it is attempted to connect them
	 * to proper rings. One assumption being used is that they are complete
	 * within the file's bounds.
	 * 
	 * It cannot distinguish between water and land tiles if there is no
	 * coastline at all (it will then guess based on the tags being used),
	 * but should be able to handle all other cases.
	 */
	public static final Collection<MapArea> createAreasForCoastlines(
			OSMData osmData, Map<OSMNode, MapNode> nodeMap,
			Collection<MapNode> mapNodes, AxisAlignedBoundingBoxXZ fileBoundary) {
		
		long highestRelationId = 0;
		long highestNodeId = 0;
		
		List<OSMWay> coastlineWays = new ArrayList<OSMWay>();
		
		for (OSMWay way : osmData.getWays()) {
			if (way.tags.contains("natural", "coastline")) {
				coastlineWays.add(way);
			}
		}
		
		for (OSMRelation relation : osmData.getRelations()) {
			if (relation.id > highestRelationId) {
				highestRelationId = relation.id;
			}
		}
		
		for (OSMNode node : osmData.getNodes()) {
			if (node.id > highestNodeId) {
				highestNodeId = node.id;
			}
		}
		
		if (fileBoundary != null) {
			
			/* build node sequences (may be closed or unclosed) */
			
			List<NodeSequence> origCoastlines = new ArrayList<NodeSequence>();
			
			for (OSMWay coastlineWay : coastlineWays) {
				origCoastlines.add(new NodeSequence(coastlineWay, nodeMap));
			}
			
			/* find coastline intersections with bounding box.
			 * They will be inserted into the rings that intersect the coastline,
			 * and into a list (sorted counterclockwise) of intersection nodes.
			 */
			
			List<NodeOnBBox> bBoxNodes = new ArrayList<NodeOnBBox>();
			
			for (final LineSegmentXZ side : getSidesClockwise(fileBoundary)) {
				
				List<NodeOnBBox> intersectionsSide =
						new ArrayList<NodeOnBBox>();
				
				for (NodeSequence coastline : origCoastlines) {
				
					for (int i = 0; i + 1 < coastline.size(); i++) {
						
						VectorXZ r1 = coastline.get(i).getPos();
						VectorXZ r2 = coastline.get(i + 1).getPos();
						
						VectorXZ intersection = getLineSegmentIntersection(
								side.p1, side.p2, r1, r2);
						
						if (intersection != null) {
						
							MapNode intersectionNode;
							
							if (intersection.equals(r1)) {
								intersectionNode = coastline.get(i);
							} else if (intersection.equals(r2)) {
								intersectionNode = coastline.get(i + 1);
							} else {
								
								intersectionNode = createFakeMapNode(intersection,
										++highestNodeId, osmData, nodeMap, mapNodes);
								
								coastline.add(i + 1, intersectionNode);
								
								i += 1;
								
							}
							
							intersectionsSide.add(new NodeOnBBox(intersectionNode,
									isRightOf(r1, side.p1, side.p2)));
							
						}
						
					}
					
				}
				
				/* add intersections for this side of the bbox,
				 * sorted by distance from corner */
				
				Collections.sort(intersectionsSide, new Comparator<NodeOnBBox>() {
					@Override public int compare(NodeOnBBox n1, NodeOnBBox n2) {
						return Double.compare(
								n1.node.getPos().distanceTo(side.p1),
								n2.node.getPos().distanceTo(side.p1));
					}
				});
				
				bBoxNodes.addAll(intersectionsSide);
				
				MapNode cornerNode = createFakeMapNode(side.p2,
						++highestNodeId, osmData, nodeMap, mapNodes);
				bBoxNodes.add(new NodeOnBBox(cornerNode, null));
				
			}
			
			/* rings are possibly shortened or split by removing all nodes
			 * outside the bbox. */
			
			List<NodeSequence> modifiedCoastlines = new ArrayList<NodeSequence>();
			
			for (NodeSequence origCoastline : origCoastlines) {
				
				NodeSequence modifiedCoastline = new NodeSequence();
				
				for (MapNode node : origCoastline) {
				
					boolean isOnBBox = false;
					
					for (NodeOnBBox bBoxNode : bBoxNodes) {
						if (bBoxNode.node.equals(node)) {
							isOnBBox = true;
						}
					}
					
					if (fileBoundary.contains(node) || isOnBBox) {
						
						modifiedCoastline.add(node);
						
					} else {
						
						if (!modifiedCoastline.isEmpty()) {
							modifiedCoastlines.add(modifiedCoastline);
							modifiedCoastline = new NodeSequence();
						}
						
					}
					
				}
				
				if (!modifiedCoastline.isEmpty()) {
					modifiedCoastlines.add(modifiedCoastline);
				}
				
			}
			
			
			/* parts of the bounding box between outgoing and incoming
			 * intersection nodes are used as additional coastline sections */
			
			List<NodeSequence> bboxSections = new ArrayList<NodeSequence>();
			
			if (bBoxNodes.size() > 4) { //more than just corners
				
				int firstIntersectionIndex = -1;
				int currentIndex = 0;
				
				List<MapNode> currentSequence = null;
				
				while (currentIndex != firstIntersectionIndex) {
				
					NodeOnBBox currentBBoxNode = bBoxNodes.get(currentIndex);
					
					if (currentBBoxNode.outgoingIntersection == TRUE) {
						
						currentSequence = new ArrayList<MapNode>();
						currentSequence.add(currentBBoxNode.node);
						
						if (firstIntersectionIndex == -1) {
							firstIntersectionIndex = currentIndex;
						}
						
					} else if (currentBBoxNode.outgoingIntersection == FALSE) {
						
						if (currentSequence != null) {
							
							currentSequence.add(currentBBoxNode.node);
							
							NodeSequence finishedBboxPart = new NodeSequence();
							finishedBboxPart.addAll(currentSequence);
							bboxSections.add(finishedBboxPart);
							
							currentSequence = null;
							
						}
						
					} else {
						
						if (currentSequence != null) {
							currentSequence.add(currentBBoxNode.node);
						}
						
					}
					
					currentIndex = (currentIndex + 1) % bBoxNodes.size();
					
				}
				
			}
			
			/* construct closed rings and turn them into polygons with holes
			 * (as if the coastlines were multipolygon member ways) */
						
			List<Ring> closedRings;
			
			if (!bboxSections.isEmpty()) {
				
				modifiedCoastlines.addAll(bboxSections);
				
				closedRings = buildRings(modifiedCoastlines);
				
			} else {
			
				closedRings = buildRings(modifiedCoastlines);
				
				if (closedRings != null) {
					
					/* if there is an island, but no coastline intersects
					 * the boundary, create a boundary around the entire tile.
					 * Do the same for water tiles (tiles without any land). */
					
					boolean hasIsland = false;
					
					for (Ring closedRing : closedRings) {
						if (!closedRing.getPolygon().isClockwise()) {
							hasIsland = true;
							break;
						}
					}
					
					if (hasIsland || isProbablySeaTile(osmData)) {
						
						NodeSequence boundaryRing = new NodeSequence();
						
						for (VectorXZ pos : fileBoundary.polygonXZ().getVertices()) {
							boundaryRing.add(createFakeMapNode(pos,
									++highestNodeId, osmData, nodeMap, mapNodes));
						}
						
						boundaryRing.add(boundaryRing.get(0));
						
						closedRings.add(new Ring(boundaryRing));
						
					}
					
				}
				
			}
			
			if (closedRings != null) {
				
				OSMRelation relation = new OSMRelation(new MapBasedTagGroup(
						new Tag("type", "multipolygon"), new Tag("natural", "water")),
						highestRelationId + 1, 0);
				
				return buildPolygonsFromRings(relation, closedRings);
				
			}
			
		}
		
		return emptyList();
		
	}

	private static final List<LineSegmentXZ> getSidesClockwise(
			AxisAlignedBoundingBoxXZ fileBoundary) {
		
		return asList(
				new LineSegmentXZ(fileBoundary.topLeft(), fileBoundary.topRight()),
				new LineSegmentXZ(fileBoundary.topRight(), fileBoundary.bottomRight()),
				new LineSegmentXZ(fileBoundary.bottomRight(), fileBoundary.bottomLeft()),
				new LineSegmentXZ(fileBoundary.bottomLeft(), fileBoundary.topLeft()));
		
	}

	private static MapNode createFakeMapNode(VectorXZ pos, long nodeId,
			OSMData osmData, Map<OSMNode, MapNode> nodeMap,
			Collection<MapNode> mapNodes) {
		
		OSMNode osmNode = new OSMNode(NaN, NaN,
				COASTLINE_NODE_TAGS, nodeId + 1);
		osmData.getNodes().add(osmNode);
				
		MapNode mapNode = new MapNode(pos, osmNode);
		mapNodes.add(mapNode);
		nodeMap.put(osmNode, mapNode);
		
		return mapNode;
		
	}
	
	/**
	 * guesses whether this is a pure sea tile (no land at all)
	 */
	private static boolean isProbablySeaTile(OSMData osmData) {
		
		boolean anySeaTag = false;
		
		Ruleset ruleset = new HardcodedRuleset();
		
		@SuppressWarnings("unchecked")
		List<Collection<? extends OSMElement>> collections = asList(
				osmData.getWays(), osmData.getNodes());
		
		for (Collection<? extends OSMElement> collection : collections) {
			for (OSMElement element : collection) {
				for (Tag tag : element.tags) {
					
					if (ruleset.isLandTag(tag)) return false;
					
					anySeaTag |= ruleset.isSeaTag(tag);
					
				}
			}
		}
		
		return anySeaTag;
		
	}
	
	private static final class NodeSequence extends ArrayList<MapNode> {
		
		/**
		 * creates an empty sequence
		 */
		public NodeSequence() {
			super();
		}
		
		/**
		 * creates a node sequence from an {@link OSMWay}
		 */
		public NodeSequence(OSMWay way, Map<OSMNode, MapNode> nodeMap) {
			
			super(way.nodes.size());
			
			for (OSMNode wayNode : way.nodes) {
				add(nodeMap.get(wayNode));
			}
			
		}
		
		/**
		 * tries to add another sequence onto the start or end of this one.
		 * If it succeeds, the other sequence may also be modified and
		 * should be considered "spent".
		 */
		public boolean tryAdd(NodeSequence other) {
			
			if (getLastNode() == other.getFirstNode()) {
				
				//add the sequence at the end
				remove(size() - 1);
				addAll(other);
				return true;
				
			} else if (getLastNode() == other.getLastNode()) {
				
				//add the sequence backwards at the end
				remove(size() - 1);
				Collections.reverse(other);
				addAll(other);
				return true;
				
			} else if (getFirstNode() == other.getLastNode()) {
				
				//add the sequence at the beginning
				remove(0);
				addAll(0, other);
				return true;
				
			} else if (getFirstNode() == other.getFirstNode()) {
				
				//add the sequence backwards at the beginning
				remove(0);
				Collections.reverse(other);
				addAll(0, other);
				return true;
				
			} else {
				return false;
			}
			
		}

		private MapNode getFirstNode() {
			return get(0);
		}

		private MapNode getLastNode() {
			return get(size() - 1);
		}

		public boolean isClosed() {
			return getFirstNode() == getLastNode();
		}
		
	}
	
	private static final class Ring implements IntersectionTestObject {
		
		private final NodeSequence closedNodeSequence;
		private final SimplePolygonXZ polygon;
		
		public Ring(NodeSequence closedNodeSequence) {

			assert closedNodeSequence.isClosed();
			
			this.closedNodeSequence = closedNodeSequence;
			
			polygon = MapArea.polygonFromMapNodeLoop(closedNodeSequence);
			
		}
		
		@Override
		public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
			
			double minX = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
			
			for (MapNode n : closedNodeSequence) {
				minX = min(minX, n.getPos().x); minZ = min(minZ, n.getPos().z);
				maxX = max(maxX, n.getPos().x); maxZ = max(maxZ, n.getPos().z);
			}
			
			return new AxisAlignedBoundingBoxXZ(minX, minZ, maxX, maxZ);
			
		}
		
		private List<MapNode> getNodeLoop() {
			return closedNodeSequence;
		}
		
		public SimplePolygonXZ getPolygon() {
			return polygon;
		}
		
		public boolean containsRing(Ring other) {
			return this.getPolygon().contains(other.getPolygon());
		}
		
	}
	
	private static class NodeOnBBox {
		
		/** true for outgoing, false for incoming, null for other bbox nodes */
		public final Boolean outgoingIntersection;
		
		public final MapNode node;
		
		private NodeOnBBox(MapNode node, Boolean outgoingIntersection) {
			this.node = node;
			this.outgoingIntersection = outgoingIntersection;
		}
		
		@Override
		public String toString() {
			return "(" + outgoingIntersection + ", " + node.getOsmNode().id +
					"@" + node.getPos() + ")";
		}
		
	}
	
}