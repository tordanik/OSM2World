package org.osm2world.core.map_data.creation;

import static de.topobyte.osm4j.core.model.util.OsmModelUtil.getTagsAsList;
import static de.topobyte.osm4j.core.model.util.OsmModelUtil.getTagsAsMap;
import static de.topobyte.osm4j.core.model.util.OsmModelUtil.isClosed;
import static de.topobyte.osm4j.core.model.util.OsmModelUtil.nodesAsList;
import static java.util.Collections.emptyList;
import static org.osm2world.core.math.VectorXZ.distance;
import static org.osm2world.core.util.FaultTolerantIterationUtil.iterate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.openstreetmap.josm.plugins.graphview.core.data.osmosis.OSMFileDataSource;
import org.osm2world.core.map_data.creation.index.MapDataIndex;
import org.osm2world.core.map_data.creation.index.MapIntersectionGrid;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapAreaSegment;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.overlaps.MapIntersectionWW;
import org.osm2world.core.map_data.data.overlaps.MapOverlapAA;
import org.osm2world.core.map_data.data.overlaps.MapOverlapNA;
import org.osm2world.core.map_data.data.overlaps.MapOverlapType;
import org.osm2world.core.map_data.data.overlaps.MapOverlapWA;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.osm.ruleset.HardcodedRuleset;
import org.osm2world.core.osm.ruleset.Ruleset;
import org.osm2world.core.util.FaultTolerantIterationUtil.Operation;

import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.core.resolve.OsmEntityProvider;

/**
 * converts {@link OSMData} into the internal map data representation
 */
public class OSMToMapDataConverter {

	private final Ruleset ruleset = new HardcodedRuleset();
	
	private final MapProjection mapProjection;
	private final Configuration config;
	
	private static final Tag MULTIPOLYON_TAG = new Tag("type", "multipolygon");
	
		
	public OSMToMapDataConverter(MapProjection mapProjection, Configuration config) {
		this.mapProjection = mapProjection;
		this.config = config;
	}

	public MapData createMapData(OSMData osmData) throws IOException, EntityNotFoundException {
		
		final List<MapNode> mapNodes = new ArrayList<MapNode>();
		final List<MapWaySegment> mapWaySegs = new ArrayList<MapWaySegment>();
		final List<MapArea> mapAreas = new ArrayList<MapArea>();
		
		createMapElements(osmData, mapNodes, mapWaySegs, mapAreas);
		
		MapData mapData = new MapData(mapNodes, mapWaySegs, mapAreas,
				calculateFileBoundary(osmData.getBounds()));
		
		calculateIntersectionsInMapData(mapData);

		return mapData;

	}

	/**
	 * creates {@link MapElement}s
	 * based on OSM data from an {@link OSMFileDataSource}
	 * and adds them to collections
	 * @throws EntityNotFoundException 
	 */
	private void createMapElements(final OSMData osmData,
			final List<MapNode> mapNodes, final List<MapWaySegment> mapWaySegs,
			final List<MapArea> mapAreas) throws EntityNotFoundException {
		
		/* create MapNode for each OSM node */

		final Map<OsmNode, MapNode> nodeMap = new HashMap<OsmNode, MapNode>();

		for (OsmNode node : osmData.getData().getNodes().valueCollection()) {
			VectorXZ nodePos = mapProjection.calcPos(node.getLatitude(), node.getLongitude());
			MapNode mapNode = new MapNode(nodePos, node);
			mapNodes.add(mapNode);
			nodeMap.put(node, mapNode);
		}
		
		/* create areas ... */
		
		final Map<OsmWay, MapArea> areaMap = new HashMap<OsmWay, MapArea>();
				
		/* ... based on multipolygons */
		
		iterate(osmData.getData().getRelations().valueCollection(), new Operation<OsmRelation>() {
			@Override public void perform(OsmRelation relation) {
				
				Map<String, String> tags = getTagsAsMap(relation);
				
				String value = tags.get(MULTIPOLYON_TAG.getKey());
				if (!MULTIPOLYON_TAG.getValue().equals(value)) {
					return;
				}
					
				try {
					for (MapArea area : MultipolygonAreaBuilder
							.createAreasForMultipolygon(relation, nodeMap, osmData.getEntityProvider())) {

						mapAreas.add(area);

						for (MapNode boundaryMapNode : area.getBoundaryNodes()) {
							boundaryMapNode.addAdjacentArea(area);
						}

						if (area.getOsmObject() instanceof OsmWay) {
							areaMap.put((OsmWay) area.getOsmObject(), area);
						}

					}
				} catch (EntityNotFoundException e) {
					// TODO: what to do here?
				}
				
			}
		});
		
		/* ... based on coastline ways */
		
		for (MapArea area : MultipolygonAreaBuilder.createAreasForCoastlines(
				osmData, nodeMap, mapNodes,
				calculateFileBoundary(osmData.getBounds()), osmData.getEntityProvider())) {
			
			mapAreas.add(area);
			
			for (MapNode boundaryMapNode : area.getBoundaryNodes()) {
				boundaryMapNode.addAdjacentArea(area);
			}
			
		}
		
		/* ... based on closed ways */
		
		OsmEntityProvider ep = osmData.getEntityProvider();
		
		for (OsmWay way : osmData.getData().getWays().valueCollection()) {
			if (isClosed(way) && !areaMap.containsKey(way)) {
				//create MapArea only if at least one tag is an area tag
				for (OsmTag tag : getTagsAsList(way)) {
					if (ruleset.isAreaTag(tag)) {
						//TODO: check whether this is old-style MP outer
						
						List<MapNode> nodes = new ArrayList<MapNode>(way.getNumberOfNodes());
						for (long id : nodesAsList(way).toArray()) {
							OsmNode boundaryOSMNode;
							try {
								boundaryOSMNode = ep.getNode(id);
								nodes.add(nodeMap.get(boundaryOSMNode));
							} catch (EntityNotFoundException e) {
								// TODO handle better sometime...
							}
						}
						
						try {
							
							MapArea mapArea = new MapArea(way, nodes);
							
							mapAreas.add(mapArea);
							areaMap.put(way, mapArea);
							
						} catch (InvalidGeometryException e) {
							System.err.println(e);
						}
						
						break;
					}
				}
			}
		}
		
		/* ... for empty terrain */
		
		AxisAlignedBoundingBoxXZ terrainBoundary =
				calculateFileBoundary(osmData.getBounds());
		
		if (terrainBoundary != null
				&& config.getBoolean("createTerrain", true)) {
			
			EmptyTerrainBuilder.createAreasForEmptyTerrain(
					mapNodes, mapAreas, terrainBoundary);
			
		} else {
			
			//TODO fall back on data boundary if file does not contain bounds
			
		}
				
		/* finish calculations */
		
		for (MapNode node : nodeMap.values()) {
			node.calculateAdjacentAreaSegments();
		}
		
		/* create way segments from remaining ways */
		
		for (OsmWay way : osmData.getData().getWays().valueCollection()) {
			boolean hasTags = way.getNumberOfTags() != 0;
			if (hasTags && !areaMap.containsKey(way)) {
				
				OsmNode previousNode = null;
				for (long id : nodesAsList(way).toArray()) {
					OsmNode node = null;
					try {
						node = ep.getNode(id);
					} catch (EntityNotFoundException e) {
						// TODO handle better sometime
						continue;
					}
					if (previousNode != null) {
						
						MapWaySegment mapWaySeg = new MapWaySegment(
								way, nodeMap.get(previousNode), nodeMap.get(node));
						
						mapWaySegs.add(mapWaySeg);
						nodeMap.get(previousNode).addOutboundLine(mapWaySeg);
						nodeMap.get(node).addInboundLine(mapWaySeg);
						
					}
					previousNode = node;
				}
				
			}
		}
		
	}
	
	/**
	 * calculates intersections and adds the information to the
	 * {@link MapElement}s
	 */
	private static void calculateIntersectionsInMapData(MapData mapData) {
		
		MapDataIndex index = new MapIntersectionGrid(mapData.getDataBoundary());
		
		for (MapElement e1 : mapData.getMapElements()) {
			
			/* collect all nearby elements */
			
			Collection<? extends Iterable<MapElement>> leaves
					= index.insertAndProbe(e1);
			
			Iterable<MapElement> nearbyElements;
			
			if (leaves.size() == 1) {
				nearbyElements = leaves.iterator().next();
			} else {
				// collect and de-duplicate elements from all the leaves
				Set<MapElement> elementSet = new HashSet<MapElement>();
				for (Iterable<MapElement> leaf : leaves) {
					for (MapElement e : leaf) {
						elementSet.add(e);
					}
				}
				nearbyElements = elementSet;
			}
			
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
	private static void addOverlapBetween(MapElement e1, MapElement e2) {
		
		if (e1 instanceof MapWaySegment
				&& e2 instanceof MapWaySegment) {
			
			addOverlapBetween((MapWaySegment) e1, (MapWaySegment) e2);
			
		} else if (e1 instanceof MapWaySegment
				&& e2 instanceof MapArea) {
			
			addOverlapBetween((MapWaySegment) e1, (MapArea) e2);
			
		} else if (e1 instanceof MapArea
				&& e2 instanceof MapWaySegment) {
			
			addOverlapBetween((MapWaySegment) e2, (MapArea) e1);
			
		} else if (e1 instanceof MapArea
				&& e2 instanceof MapArea) {
			
			addOverlapBetween((MapArea) e1, (MapArea) e2);
			
		} else if (e1 instanceof MapNode
				&& e2 instanceof MapArea) {
			
			addOverlapBetween((MapNode) e1, (MapArea) e2);
			
		} else if (e1 instanceof MapArea
				&& e2 instanceof MapNode) {
			
			addOverlapBetween((MapNode) e2, (MapArea) e1);
			
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

	/**
	 * adds the overlap between two {@link MapArea}s
	 * to both, if it exists
	 */
	private static void addOverlapBetween(
			MapArea area1, MapArea area2) {
		
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
			
			Set<VectorXZ> commonNodes = new HashSet<VectorXZ>();
			for (SimplePolygonXZ p : polygon1.getPolygons()) {
				commonNodes.addAll(p.getVertices());
			}
			
			Set<VectorXZ> nodes2 = new HashSet<VectorXZ>();
			for (SimplePolygonXZ p : polygon2.getPolygons()) {
				nodes2.addAll(p.getVertices());
			}
			
			commonNodes.retainAll(nodes2);
			
			/* check whether the areas' outlines intersects somewhere
			 * else than just at the common node(s).
			 */
			
			intersectionPosCheck:
			for (VectorXZ pos : polygon1.intersectionPositions(polygon2)) {
				boolean trueIntersection = true;
				for (VectorXZ commonNode : commonNodes) {
					if (distance(pos, commonNode) < 0.01) {
						trueIntersection = false;
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
	
	private AxisAlignedBoundingBoxXZ calculateFileBoundary(
			Collection<OsmBounds> bounds) {
		
		Collection<VectorXZ> boundedPoints = new ArrayList<VectorXZ>();
		
		for (OsmBounds bound : bounds) {
			
			boundedPoints.add(mapProjection.calcPos(bound.getBottom(), bound.getLeft()));
			boundedPoints.add(mapProjection.calcPos(bound.getBottom(), bound.getRight()));
			boundedPoints.add(mapProjection.calcPos(bound.getTop(), bound.getLeft()));
			boundedPoints.add(mapProjection.calcPos(bound.getTop(), bound.getRight()));
			
		}
		
		if (boundedPoints.isEmpty()) {
			return null;
		} else {
			return new AxisAlignedBoundingBoxXZ(boundedPoints);
		}
		
	}

}
