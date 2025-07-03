package org.osm2world.map_data.creation;

import static de.topobyte.osm4j.core.model.util.OsmModelUtil.*;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparingDouble;
import static org.osm2world.map_data.creation.OSMToMapDataConverter.tagsOfEntity;
import static org.osm2world.map_data.creation.OSMToMapDataConverter.wayNodes;
import static org.osm2world.math.algorithms.GeometryUtil.getLineSegmentIntersection;
import static org.osm2world.math.algorithms.GeometryUtil.isRightOf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapAreaSegment;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.BoundedObject;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.osm.data.OSMData;
import org.osm2world.osm.ruleset.HardcodedRuleset;
import org.osm2world.osm.ruleset.Ruleset;
import org.osm2world.util.exception.InvalidGeometryException;

import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.model.impl.Relation;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.core.resolve.OsmEntityProvider;
import gnu.trove.map.TLongObjectMap;

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
	 * {@link MapNode#addAdjacentArea(MapArea, MapAreaSegment)}.
	 *
	 * @param relation  the multipolygon relation
	 * @param nodeIdMap   map from node ids to {@link MapNode}s
	 *
	 * @return  constructed area(s), multiple areas will be created if there
	 *          is more than one outer ring. Empty for invalid multipolygons.
	 */
	public static final Collection<MapArea> createAreasForMultipolygon(OsmRelation relation,
			TLongObjectMap<MapNode> nodeIdMap, OsmEntityProvider db) throws EntityNotFoundException {

		if (isSimpleMultipolygon(relation, db)) {
			return createAreasForSimpleMultipolygon(relation, nodeIdMap, db);
		} else {
			return createAreasForAdvancedMultipolygon(relation, nodeIdMap, db);
		}

	}

	private static final boolean isSimpleMultipolygon(OsmRelation relation, OsmEntityProvider db) throws EntityNotFoundException {

		int numberOuters = 0;
		boolean closedWays = true;

		for (OsmRelationMember member :membersAsList(relation)) {

			if ("outer".equals(member.getRole())
					&& member.getType() == EntityType.Way) {
				numberOuters += 1;
			}

			if (("outer".equals(member.getRole()) || "inner".equals(member.getRole()))
					&& member.getType() == EntityType.Way) {

				OsmWay way = db.getWay(member.getId());
				if (!isClosed(way)) {
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
	 * {@link #createAreasForAdvancedMultipolygon(OsmRelation, TLongObjectMap, OsmEntityProvider)}
	 *
	 * @param relation  has to be a simple multipolygon relation
	 */
	private static final Collection<MapArea> createAreasForSimpleMultipolygon(OsmRelation relation,
			TLongObjectMap<MapNode> nodeIdMap, OsmEntityProvider db) throws EntityNotFoundException {

		try {
			assert isSimpleMultipolygon(relation, db);

			OsmEntity tagSource = null;
			List<MapNode> outerNodes = null;
			List<List<MapNode>> holes = new ArrayList<List<MapNode>>();

			for (OsmRelationMember member : membersAsList(relation)) {
				if (member.getType() == EntityType.Way) {

					OsmWay way = db.getWay(member.getId());

					if ("inner".equals(member.getRole())) {
						holes.add(wayNodes(way, nodeIdMap));
					} else if ("outer".equals(member.getRole())) {
						tagSource = relation.getNumberOfTags() > 1 ? relation : way;
						outerNodes = wayNodes(way, nodeIdMap);
					}

				}
			}

			return singleton(new MapArea(tagSource.getId(), tagSource instanceof OsmRelation,
					tagsOfEntity(tagSource), outerNodes, holes));

		} catch (EntityNotFoundException e) {
			throw new EntityNotFoundException(e);
		}

	}

	private static final Collection<MapArea> createAreasForAdvancedMultipolygon(OsmRelation relation,
			TLongObjectMap<MapNode> nodeIdMap, OsmEntityProvider db) throws EntityNotFoundException {

		List<NodeSequence> innersAndOuters = new ArrayList<NodeSequence>();

		/* collect ways */

		for (OsmRelationMember member : membersAsList(relation)) {
			if (member.getType() == EntityType.Way
					&& ("outer".equals(member.getRole())
							|| "inner".equals(member.getRole())) ) {

				OsmWay way = db.getWay(member.getId());
				innersAndOuters.add(new NodeSequence(way, nodeIdMap));

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
				try {
					closedRings.add(new Ring(currentRing));
					currentRing = null;
				} catch (InvalidGeometryException e) {
					throw new InvalidGeometryException(String.format(
							"self-intersecting ring (with %d nodes)",
							currentRing.size() - 1), e);
				}
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
	private static final List<MapArea> buildPolygonsFromRings(
			OsmRelation relation, List<Ring> rings) {

		List<MapArea> finishedPolygons = new ArrayList<>(rings.size() / 2);

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

			MapArea area = new MapArea(relation.getId(), true, tagsOfEntity(relation),
					outerRing.getNodeLoop(), holes, new PolygonWithHolesXZ(outerRing.getPolygon(), holesXZ));

			finishedPolygons.add(area);

			rings.remove(outerRing);
			rings.removeAll(innerRings);

		}

		return finishedPolygons;

	}

	private static final TagSet COASTLINE_NODE_TAGS = TagSet.of(
			"osm2world:note", "fake node from coastline processing");

	/**
	 * turns all coastline ways into {@link MapArea}s
	 * based on an artificial natural=water multipolygon relation.
	 * <p>
	 * It relies on the direction-dependent drawing of coastlines.
	 * If coastlines are incomplete, then it is attempted to connect them
	 * to proper rings. One assumption being used is that they are complete
	 * within the file's bounds.
	 * <p>
	 * It cannot distinguish between water and land tiles if there is no
	 * coastline at all and no explicit information is provided,
	 * but should be able to handle all other cases.
	 *
	 * @param isAtSea  true if the {@link OSMData} is sea on all sides (it may contain islands as long as they are
	 *                 entirely within the bounds); false if it's on land or unknown/mixed
	 */
	public static final List<MapArea> createAreasForCoastlines(
			OSMData osmData, TLongObjectMap<MapNode> nodeIdMap,
			Collection<MapNode> mapNodes, AxisAlignedRectangleXZ fileBoundary,
			boolean isAtSea) throws EntityNotFoundException {

		long highestRelationId = 0;
		long highestNodeId = 0;

		List<OsmWay> coastlineWays = new ArrayList<OsmWay>();

		for (OsmWay way : osmData.getWays()) {
			if ("coastline".equals(getTagsAsMap(way).get("natural"))) {
				coastlineWays.add(way);
			}
		}

		for (OsmRelation relation : osmData.getRelations()) {
			if (relation.getId() > highestRelationId) {
				highestRelationId = relation.getId();
			}
		}

		for (OsmNode node : osmData.getNodes()) {
			if (node.getId() > highestNodeId) {
				highestNodeId = node.getId();
			}
		}

		if (fileBoundary != null) {

			/* build node sequences (may be closed or unclosed) */

			List<NodeSequence> origCoastlines = new ArrayList<NodeSequence>();

			for (OsmWay coastlineWay : coastlineWays) {
				origCoastlines.add(new NodeSequence(coastlineWay, nodeIdMap));
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
										++highestNodeId, nodeIdMap, mapNodes);

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

				intersectionsSide.sort(comparingDouble(n -> n.node.getPos().distanceTo(side.p1)));

				bBoxNodes.addAll(intersectionsSide);

				MapNode cornerNode = createFakeMapNode(side.p2,
						++highestNodeId, nodeIdMap, mapNodes);
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

					if (fileBoundary.contains(node.getPos()) || isOnBBox) {

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

					if (hasIsland || isAtSea || isProbablySeaTile(osmData)) {

						NodeSequence boundaryRing = new NodeSequence();

						for (VectorXZ pos : fileBoundary.polygonXZ().getVertices()) {
							boundaryRing.add(createFakeMapNode(pos,
									++highestNodeId, nodeIdMap, mapNodes));
						}

						boundaryRing.add(boundaryRing.get(0));

						closedRings.add(new Ring(boundaryRing));

					}

				}

			}

			if (closedRings != null) {

				List<OsmTag> tags = new ArrayList<>();

				tags.add(new Tag("type", "multipolygon"));
				tags.add(new Tag("natural", "water"));

				List<? extends OsmRelationMember> members = new ArrayList<>();
				OsmRelation relation = new Relation(highestRelationId + 1, members, tags);

				return buildPolygonsFromRings(relation, closedRings);

			}

		}

		return emptyList();

	}

	private static final List<LineSegmentXZ> getSidesClockwise(
			AxisAlignedRectangleXZ fileBoundary) {

		return asList(
				new LineSegmentXZ(fileBoundary.topLeft(), fileBoundary.topRight()),
				new LineSegmentXZ(fileBoundary.topRight(), fileBoundary.bottomRight()),
				new LineSegmentXZ(fileBoundary.bottomRight(), fileBoundary.bottomLeft()),
				new LineSegmentXZ(fileBoundary.bottomLeft(), fileBoundary.topLeft()));

	}

	private static MapNode createFakeMapNode(VectorXZ pos, long nodeId,
			TLongObjectMap<MapNode> nodeIdMap, Collection<MapNode> mapNodes) {

		long id = nodeId + 1;
		MapNode mapNode = new MapNode(id, COASTLINE_NODE_TAGS, pos);
		mapNodes.add(mapNode);
		nodeIdMap.put(id, mapNode);

		return mapNode;

	}

	/**
	 * guesses whether this is a pure sea tile (no land at all)
	 */
	private static boolean isProbablySeaTile(OSMData osmData) {

		boolean anySeaTag = false;

		Ruleset ruleset = new HardcodedRuleset();

		List<Collection<? extends OsmEntity>> collections = asList(osmData.getWays(), osmData.getNodes());

		for (Collection<? extends OsmEntity> collection : collections) {
			for (OsmEntity element : collection) {
				for (OsmTag tag : getTagsAsList(element)) {

					if (ruleset.isLandTag(tag)) return false;

					anySeaTag |= ruleset.isSeaTag(tag);

				}
			}
		}

		return anySeaTag;

	}

	private static final class NodeSequence extends ArrayList<MapNode> {

		private static final long serialVersionUID = -1189277554247756781L; //generated SerialUID

		/**
		 * creates an empty sequence
		 */
		public NodeSequence() {
			super();
		}

		/**
		 * creates a node sequence from an {@link OsmWay}
		 */
		public NodeSequence(OsmWay way, TLongObjectMap<MapNode> nodeIdMap) throws EntityNotFoundException {
			super(way.getNumberOfNodes());
			addAll(wayNodes(way, nodeIdMap));
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

	private static final class Ring implements BoundedObject {

		private final NodeSequence closedNodeSequence;
		private final SimplePolygonXZ polygon;

		public Ring(NodeSequence closedNodeSequence) {

			assert closedNodeSequence.isClosed();

			this.closedNodeSequence = closedNodeSequence;

			polygon = MapArea.polygonFromMapNodeLoop(closedNodeSequence);

		}

		@Override
		public AxisAlignedRectangleXZ boundingBox() {

			double minX = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

			for (MapNode n : closedNodeSequence) {
				minX = min(minX, n.getPos().x); minZ = min(minZ, n.getPos().z);
				maxX = max(maxX, n.getPos().x); maxZ = max(maxZ, n.getPos().z);
			}

			return new AxisAlignedRectangleXZ(minX, minZ, maxX, maxZ);

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
			return "(" + outgoingIntersection + ", " + node.getId() + "@" + node.getPos() + ")";
		}

	}

}