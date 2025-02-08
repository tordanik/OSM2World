package org.osm2world.map_data.creation;

import static de.topobyte.osm4j.core.model.util.OsmModelUtil.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.osm2world.map_data.data.MapRelation.Membership;
import static org.osm2world.math.VectorXZ.distance;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bbox;
import static org.osm2world.util.FaultTolerantIterationUtil.forEach;

import java.util.*;

import javax.annotation.Nullable;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.map_data.data.*;
import org.osm2world.map_data.data.overlaps.*;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.GeometryUtil;
import org.osm2world.math.datastructures.IndexGrid;
import org.osm2world.math.datastructures.SpatialIndex;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.osm.data.OSMData;
import org.osm2world.osm.ruleset.HardcodedRuleset;
import org.osm2world.osm.ruleset.Ruleset;
import org.osm2world.util.exception.InvalidGeometryException;

import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

/**
 * converts {@link OSMData} into the internal map data representation
 */
public class OSMToMapDataConverter {

	private final Ruleset ruleset = new HardcodedRuleset();

	private final MapProjection mapProjection;

	private static final Tag MULTIPOLYON_TAG = new Tag("type", "multipolygon");


	public OSMToMapDataConverter(MapProjection mapProjection) {
		this.mapProjection = mapProjection;
	}

	public MapData createMapData(OSMData osmData, @Nullable MapMetadata metadata) throws EntityNotFoundException {

		final List<MapNode> mapNodes = new ArrayList<>();
		final List<MapWay> mapWays = new ArrayList<>();
		final List<MapArea> mapAreas = new ArrayList<>();
		final List<MapRelation> mapRelations = new ArrayList<>();

		if (metadata == null) {
			metadata = new MapMetadata(null, null);
		}

		createMapElements(osmData, metadata, mapNodes, mapWays, mapAreas, mapRelations);

		MapData mapData = new MapData(mapNodes, mapWays, mapAreas, mapRelations,
				calculateFileBoundary(osmData.getUnionOfExplicitBounds()), metadata);

		calculateIntersectionsInMapData(mapData);

		return mapData;

	}

	/**
	 * creates {@link MapElement}s
	 * based on OSM data from an {@link OSMData} dataset.
	 * and adds them to collections
	 */
	private void createMapElements(final OSMData osmData, MapMetadata metadata,
			final List<MapNode> mapNodes, final List<MapWay> mapWays,
			final List<MapArea> mapAreas, List<MapRelation> mapRelations) throws EntityNotFoundException {

		/* create MapNode for each OSM node */

		final TLongObjectMap<MapNode> nodeIdMap = new TLongObjectHashMap<>();

		for (OsmNode node : osmData.getNodes()) {
			VectorXZ nodePos = mapProjection.toXZ(node.getLatitude(), node.getLongitude());
			MapNode mapNode = new MapNode(node.getId(), tagsOfEntity(node), nodePos);
			mapNodes.add(mapNode);
			nodeIdMap.put(node.getId(), mapNode);
		}

		/* create areas ... */

		final Map<Long, MapArea> areaMap = new HashMap<>();

		/* ... based on multipolygons */

		forEach(osmData.getRelations(), (OsmRelation relation) -> {

			TagSet tags = TagSet.of(getTagsAsMap(relation));

			if (!tags.contains(MULTIPOLYON_TAG)) {
				return;
			}

			try {

				Collection<MapArea> areas = MultipolygonAreaBuilder
						.createAreasForMultipolygon(relation, nodeIdMap, osmData);

				if (areas.size() > 1) {
					// create a relation object to link the areas created from multiple outer rings
					mapRelations.add(new MapMultipolygonRelation(relation.getId(), tags, areas));
				}

				for (MapArea area : areas) {

					mapAreas.add(area);

					if (!area.isBasedOnRelation()) {
						areaMap.put(area.getId(), area);
					}

				}

			} catch (EntityNotFoundException e) {
				// skip this area
			}

		});

		/* ... based on coastline ways */

		mapAreas.addAll(MultipolygonAreaBuilder.createAreasForCoastlines(
				osmData, nodeIdMap, mapNodes,
				calculateFileBoundary(osmData.getUnionOfExplicitBounds()),
				metadata.land() == Boolean.FALSE));

		/* ... based on closed ways with certain tags */

		for (OsmWay way : osmData.getWays()) {
			if (isClosed(way) && !areaMap.containsKey(way.getId())) {
				TagSet tags = tagsOfEntity(way);
				if (!tags.contains("area", "no")
						&& tags.stream().anyMatch(ruleset::isAreaTag)) {

					try {

						List<MapNode> nodes = wayNodes(way, nodeIdMap);

						MapArea mapArea = new MapArea(way.getId(), false, tags, nodes);

						mapAreas.add(mapArea);
						areaMap.put(way.getId(), mapArea);

					} catch (EntityNotFoundException | InvalidGeometryException e) {
						ConversionLog.error(e.getMessage());
					}

				}
			}
		}

		/* ... for empty terrain */

		AxisAlignedRectangleXZ terrainBoundary = calculateFileBoundary(osmData.getUnionOfExplicitBounds());

		if (terrainBoundary != null) {

			EmptyTerrainBuilder.createAreasForEmptyTerrain(
					mapNodes, mapAreas, terrainBoundary);

		} else {

			//TODO fall back on data boundary if file does not contain bounds

		}

		/* finish calculations */

		for (MapNode node : mapNodes) {
			node.calculateAdjacentAreaSegments();
		}

		/* create ways from remaining OSM ways */

		for (OsmWay osmWay : osmData.getWays()) {
			boolean hasTags = osmWay.getNumberOfTags() != 0;
			if (hasTags && !areaMap.containsKey(osmWay.getId())) {
				try {
					List<MapNode> nodes = wayNodes(osmWay, nodeIdMap);
					var way = new MapWay(osmWay.getId(), tagsOfEntity(osmWay), nodes);
					mapWays.add(way);
				} catch (EntityNotFoundException e) {
					ConversionLog.error(e.getMessage());
				}
			}
		}

		/* create relations from the remaining relevant OSM relations */

		TLongObjectMap<MapRelationElement> wayIdMap = new TLongObjectHashMap<>();
		TLongObjectMap<MapRelationElement> relationIdMap = new TLongObjectHashMap<>();

		for (MapWay way : mapWays) {
			wayIdMap.put(way.getId(), way);
		}

		for (MapArea area : mapAreas) {
			var parentMultipolygon = area.getMemberships().stream().map(Membership::getRelation)
					.filter(it -> it.getTags().contains(MULTIPOLYON_TAG)).findAny();
			if (parentMultipolygon.isPresent()) {
				relationIdMap.put(parentMultipolygon.get().getId(), parentMultipolygon.get());
			} else if (!area.isBasedOnRelation()) {
				wayIdMap.put(area.getId(), area);
			} else {
				relationIdMap.put(area.getId(), area);
			}
		}

		for (OsmRelation osmRelation : osmData.getRelations()) {
			boolean hasTags = osmRelation.getNumberOfTags() != 0;
			if (hasTags && !relationIdMap.containsKey(osmRelation.getId())) {

				MapRelation relation = new MapRelation(osmRelation.getId(), tagsOfEntity(osmRelation));

				if (!ruleset.isRelevantRelation(relation.getTags())) {
					continue;
				}

				List<OsmRelationMember> incompleteMembers = null;

				for (OsmRelationMember osmMember : membersAsList(osmRelation)) {

					MapRelationElement element = null;

					switch (osmMember.getType()) {
					case Node:
						element = nodeIdMap.get(osmMember.getId());
						break;
					case Way:
						element = wayIdMap.get(osmMember.getId());
						break;
					case Relation:
						if (relationIdMap.containsKey(osmMember.getId())) {
							element = relationIdMap.get(osmMember.getId());
							break;
						} else {
							//TODO: support relations containing other (non-multipolygon) relations as members
							continue;
						}
					}

					if (element == null) {
						if (incompleteMembers == null) { incompleteMembers = new ArrayList<>(); }
						incompleteMembers.add(osmMember);
						continue;
					}

					relation.addMember(osmMember.getRole(), element);

				}

				if (incompleteMembers != null) {

					StringJoiner memberList = new StringJoiner(", ");
					incompleteMembers.forEach(m -> memberList.add(
							"'" + m.getRole() + "': " + m.getType() + " " + m.getId()));
					ConversionLog.warn("Relation " + relation + " is incomplete, missing members: " + memberList);

					if (relation.getMembers().isEmpty()) continue;

				}

				mapRelations.add(relation);

			}
		}

	}

	public static TagSet tagsOfEntity(OsmEntity entity) {

		if (entity.getNumberOfTags() == 0) return TagSet.of();

		Tag[] tags =
				new Tag[entity.getNumberOfTags()];
		for (int i = 0; i < entity.getNumberOfTags(); i++) {
			tags[i] = new Tag(entity.getTag(i).getKey(), entity.getTag(i).getValue());
		}
		return TagSet.of(tags);

	}

	public static List<MapNode> wayNodes(OsmWay way, TLongObjectMap<MapNode> nodeIdMap) throws EntityNotFoundException {
		List<MapNode> result = new ArrayList<>(way.getNumberOfNodes());
		for (long id : nodesAsList(way).toArray()) {
			MapNode node = nodeIdMap.get(id);
			if (node != null) {
				result.add(node);
			} else {
				throw new EntityNotFoundException("Invalid input data: Way w" + way.getId()
						+ " references missing node n" + id);
			}
		}
		return result;
	}

	/**
	 * calculates intersections and adds the information to the
	 * {@link MapElement}s
	 */
	private static void calculateIntersectionsInMapData(MapData mapData) {

		AxisAlignedRectangleXZ bounds = mapData.getDataBoundary().pad(10);
		SpatialIndex<MapElement> index = new IndexGrid<>(bounds, bounds.sizeX() / 1000, bounds.sizeZ() / 1000);

		for (MapElement e1 : mapData.getMapElements()) {

			/* collect all nearby elements */

			Iterable<MapElement> nearbyElements = index.insertAndProbe(e1);

			for (MapElement e2 : nearbyElements) {

				if (e1 == e2) { continue; }

				addOverlapBetween(e1, e2);

			}

		}

	}

	/**
	 * adds the overlap between two {@link MapElement}s
	 * to both, if it exists. It calls the appropriate
	 * subtype-specific addOverlapBetween method
	 */
	static void addOverlapBetween(MapElement e1, MapElement e2) {

		if (e1 instanceof MapWaySegment s1
				&& e2 instanceof MapWaySegment s2) {

			addOverlapBetween(s1, s2);

		} else if (e1 instanceof MapWaySegment s
				&& e2 instanceof MapArea area) {

			addOverlapBetween(s, area);

		} else if (e1 instanceof MapArea area
				&& e2 instanceof MapWaySegment s) {

			addOverlapBetween(s, area);

		} else if (e1 instanceof MapArea area1
				&& e2 instanceof MapArea area2) {

			addOverlapBetween(area1, area2);

		} else if (e1 instanceof MapNode node
				&& e2 instanceof MapArea area) {

			addOverlapBetween(node, area);

		} else if (e1 instanceof MapArea area
				&& e2 instanceof MapNode node) {

			addOverlapBetween(node, area);

		}

	}

	/**
	 * adds the overlap between two {@link MapWaySegment}s
	 * to both, if it exists
	 */
	private static void addOverlapBetween(
			MapWaySegment line1, MapWaySegment line2) {

		if (line1.isConnectedTo(line2)) { return; }

		VectorXZ intersection = GeometryUtil.getLineSegmentIntersection(
				line1.getStartNode().getPos(),
				line1.getEndNode().getPos(),
				line2.getStartNode().getPos(),
				line2.getEndNode().getPos());

		if (intersection != null) {

			/* add the intersection */

			MapIntersectionWW newIntersection =
				new MapIntersectionWW(line1, line2, intersection);

			line1.addOverlap(newIntersection);
			line2.addOverlap(newIntersection);

		}

	}

	/**
	 * adds the overlap between a {@link MapWaySegment}
	 * and a {@link MapArea} to both, if it exists
	 */
	private static void addOverlapBetween(
			MapWaySegment line, MapArea area) {

		final LineSegmentXZ segmentXZ = line.getLineSegment();

		/* check whether the line corresponds to one of the area segments */

		for (MapAreaSegment areaSegment : area.getAreaSegments()) {
			if (areaSegment.sharesBothNodes(line)) {

				MapOverlapWA newOverlap =
					new MapOverlapWA(line, area, MapOverlapType.SHARE_SEGMENT,
							Collections.<VectorXZ>emptyList(),
							Collections.<MapAreaSegment>emptyList());

				line.addOverlap(newOverlap);
				area.addOverlap(newOverlap);

				return;

			}
		}

		/* calculate whether the line contains or intersects the area (or neither) */

		boolean contains;
		boolean intersects;

		{
			final PolygonWithHolesXZ polygon = area.getPolygon();

			if (!line.isConnectedTo(area)) {

				intersects = polygon.intersects(segmentXZ);
				contains = !intersects && polygon.contains(segmentXZ);

			} else {

				/* check whether the line intersects the area somewhere
				 * else than just at the common node(s).
				 */

				intersects = false;

				double segmentLength = distance(segmentXZ.p1, segmentXZ.p2);

				for (VectorXZ pos : polygon.intersectionPositions(segmentXZ)) {
					if (distance(pos, segmentXZ.p1) > segmentLength / 100
							&& distance(pos, segmentXZ.p2) > segmentLength / 100) {
						intersects = true;
						break;
					}
				}

				/* check whether the area contains the line's center.
				 * Unless the line intersects the area outline,
				 * this means that the area contains the line itself.
				 */

				contains = !intersects && polygon.contains(segmentXZ.getCenter());

			}

		}

		/* add an overlap if detected */

		if (contains || intersects) {

			/* find out which area segments intersect the way segment */

			List<VectorXZ> intersectionPositions = emptyList();
			List<MapAreaSegment> intersectingSegments = emptyList();

			if (intersects) {

				intersectionPositions = new ArrayList<VectorXZ>();
				intersectingSegments = new ArrayList<MapAreaSegment>();

				for (MapAreaSegment areaSegment : area.getAreaSegments()) {

					VectorXZ intersection = segmentXZ.getIntersection(
							areaSegment.getStartNode().getPos(),
							areaSegment.getEndNode().getPos());

					if (intersection != null) {
						intersectionPositions.add(intersection);
						intersectingSegments.add(areaSegment);
					}

				}

			}

			/* add the overlap */

			MapOverlapWA newOverlap = new MapOverlapWA(line, area,
						intersects ? MapOverlapType.INTERSECT : MapOverlapType.CONTAIN,
						intersectionPositions, intersectingSegments);

			line.addOverlap(newOverlap);
			area.addOverlap(newOverlap);

		}

	}

	/** adds the overlap between two {@link MapArea}s to both, if it exists */
	private static void addOverlapBetween(MapArea area1, MapArea area2) {

		/* check whether the areas have a shared segment */

		Collection<MapAreaSegment> area1Segments = area1.getAreaSegments();
		Collection<MapAreaSegment> area2Segments = area2.getAreaSegments();

		for (MapAreaSegment area1Segment : area1Segments) {
			for (MapAreaSegment area2Segment : area2Segments) {
				if (area1Segment.sharesBothNodes(area2Segment)) {

					MapOverlapAA newOverlap =
						new MapOverlapAA(area1, area2, MapOverlapType.SHARE_SEGMENT);
					area1.addOverlap(newOverlap);
					area2.addOverlap(newOverlap);

					return;

				}
			}
		}

		/* calculate whether one area contains the other
		 * or whether their outlines intersect (or neither) */

		boolean contains1 = false;
		boolean contains2 = false;
		boolean intersects = false;

		{
			final PolygonWithHolesXZ polygon1 = area1.getPolygon();
			final PolygonWithHolesXZ polygon2 = area2.getPolygon();

			/* determine common nodes */

			Collection<VectorXZ> commonNodes = new ArrayList<>();

			for (List<MapNode> ring : area1.getRings()) {
				for (MapNode node : ring) {
					if (node.getAdjacentAreas().contains(area2)) {
						commonNodes.add(node.getPos());
					}
				}
			}

			/* check whether the areas' outlines intersects somewhere
			 * else than just at the common node(s).
			 */

			intersectionPosCheck:
			for (VectorXZ pos : polygon1.intersectionPositions(polygon2)) {
				boolean trueIntersection = true;
				for (VectorXZ commonNode : commonNodes) {
					if (distance(pos, commonNode) < 0.01) {
						trueIntersection = false;
						break;
					}
				}
				if (trueIntersection) {
					intersects = true;
					break intersectionPosCheck;
				}
			}

			/* check whether one area contains the other */

			if (polygon1.contains(polygon2.getOuter())) {
				contains1 = true;
			} else if (polygon2.contains(polygon1.getOuter())) {
				contains2 = true;
			}

		}

		/* add an overlap if detected */

		if (contains1 || contains2 || intersects) {

			/* add the overlap */

			MapOverlapAA newOverlap = null;

			if (contains1) {
				newOverlap = new MapOverlapAA(area2, area1, MapOverlapType.CONTAIN);
			} else if (contains2) {
				newOverlap = new MapOverlapAA(area1, area2, MapOverlapType.CONTAIN);
			} else {
				newOverlap = new MapOverlapAA(area1, area2, MapOverlapType.INTERSECT);
			}

			area1.addOverlap(newOverlap);
			area2.addOverlap(newOverlap);

		}

	}

	private static void addOverlapBetween(MapNode node, MapArea area) {

		if (area.getPolygon().contains(node.getPos())) {

			/* add the overlap */

			MapOverlapNA newOverlap =
					new MapOverlapNA(node, area, MapOverlapType.CONTAIN);

			area.addOverlap(newOverlap);

		}

	}

	/** @param b  union of all explicit bounding boxes in the OSM dataset */
	private @Nullable AxisAlignedRectangleXZ calculateFileBoundary(@Nullable LatLonBounds b) {

		if (b == null) return null;

		return bbox(asList(
				mapProjection.toXZ(b.minlat, b.minlon),
				mapProjection.toXZ(b.minlat, b.maxlon),
				mapProjection.toXZ(b.maxlat, b.minlon),
				mapProjection.toXZ(b.maxlat, b.maxlon)));

	}

}
