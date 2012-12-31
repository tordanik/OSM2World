package org.osm2world.core.map_data.creation;

import static org.osm2world.core.math.VectorXZ.distance;
import static org.osm2world.core.util.FaultTolerantIterationUtil.iterate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.openstreetmap.josm.plugins.graphview.core.data.osmosis.OSMFileDataSource;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.osm2world.core.map_data.creation.index.Map2dTree;
import org.osm2world.core.map_data.creation.index.MapDataIndex;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapAreaSegment;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.overlaps.MapIntersectionWW;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_data.data.overlaps.MapOverlapAA;
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
import org.osm2world.core.osm.data.OSMElement;
import org.osm2world.core.osm.data.OSMNode;
import org.osm2world.core.osm.data.OSMRelation;
import org.osm2world.core.osm.data.OSMWay;
import org.osm2world.core.osm.ruleset.HardcodedRuleset;
import org.osm2world.core.osm.ruleset.Ruleset;
import org.osm2world.core.util.FaultTolerantIterationUtil.Operation;

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

	public MapData createMapData(OSMData osmData) throws IOException {
		
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
	 */
	private void createMapElements(OSMData osmData,
			final List<MapNode> mapNodes, final List<MapWaySegment> mapWaySegs,
			final List<MapArea> mapAreas) {
		
		/* create MapNode for each OSM node */

		final Map<OSMNode, MapNode> nodeMap = new HashMap<OSMNode, MapNode>();

		for (OSMNode node : osmData.getNodes()) {
			VectorXZ nodePos = mapProjection.calcPos(node.lat, node.lon);
			MapNode mapNode = new MapNode(nodePos, node);
			mapNodes.add(mapNode);
			nodeMap.put(node, mapNode);
		}
		
		/* create areas ... */
		
		final Map<OSMWay, MapArea> areaMap = new HashMap<OSMWay, MapArea>();
				
		/* ... based on multipolygons */
		
		iterate(osmData.getRelations(), new Operation<OSMRelation>() {
			@Override public void perform(OSMRelation relation) {
				
				if (relation.tags.contains(MULTIPOLYON_TAG)) {
					
					for (MapArea area : MultipolygonAreaBuilder.
							createAreasForMultipolygon(relation, nodeMap)) {
						
						mapAreas.add(area);
						
						for (MapNode boundaryMapNode : area.getBoundaryNodes()) {
							boundaryMapNode.addAdjacentArea(area);
						}
						
						if (area.getOsmObject() instanceof OSMWay) {
							areaMap.put((OSMWay)area.getOsmObject(), area);
						}
						
					}
					
				}
				
			}
		});
		
		/* ... based on coastline ways */
		
		for (MapArea area : MultipolygonAreaBuilder.createAreasForCoastlines(
				osmData, nodeMap, mapNodes,
				calculateFileBoundary(osmData.getBounds()))) {
			
			mapAreas.add(area);
			
			for (MapNode boundaryMapNode : area.getBoundaryNodes()) {
				boundaryMapNode.addAdjacentArea(area);
			}
			
		}
		
		/* ... based on closed ways */
		
		for (OSMWay way : osmData.getWays()) {
			if (way.isClosed() && !areaMap.containsKey(way)) {
				//create MapArea only if at least one tag is an area tag
				for (Tag tag : way.tags) {
					if (ruleset.isAreaTag(tag)) {
						//TODO: check whether this is old-style MP outer
						
						List<MapNode> nodes = new ArrayList<MapNode>(way.nodes.size());
						for (OSMNode boundaryOSMNode : way.nodes) {
							nodes.add(nodeMap.get(boundaryOSMNode));
						}
						
						try {
							
							MapArea mapArea = new MapArea((OSMElement)way, nodes);
							
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
					
		/* finish calculations */
		
		for (MapNode node : nodeMap.values()) {
			node.calculateAdjacentAreaSegments();
		}
		
		/* create way segments from remaining ways */
		
		for (OSMWay way : osmData.getWays()) {
			if (!way.tags.isEmpty() && !areaMap.containsKey(way)) {
				
				OSMNode previousNode = null;
				for (OSMNode node : way.nodes) {
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
		
		MapDataIndex index = new Map2dTree(mapData);
		
		// find intersections, using an index to reduce the number of checks
		
		for (Iterable<MapElement> leaf : index.getLeaves()) {
			
			for (MapElement e1 : leaf) {
				secondElementLoop :
				for (MapElement e2 : leaf) {
			
					//TODO: use for (int ...) loop
					
					if (e1 == e2) { continue; }
					
					/* filter out existing overlaps/intersections */
					
					for (MapOverlap<? extends MapElement, ?> e1Overlap : e1.getOverlaps()) {
						if (e1Overlap.getOther(e1) == e2) {
							continue secondElementLoop;
						}
					}
					
					/* calculate overlaps/intersections depending on element type */
					
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
						
					}
					
				}
			}
		
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

		/* check whether the line corresponds to one of the area segments */
				
		for (MapAreaSegment areaSegment : area.getAreaSegments()) {
			if (areaSegment.sharesBothNodes(line)) {
				
				MapOverlapWA newOverlap =
					new MapOverlapWA(line, area, MapOverlapType.SHARE_SEGMENT);
				line.addOverlap(newOverlap);
				area.addOverlap(newOverlap);
				
				return;
				
			}
		}
		
		/* calculate whether the line contains or intersects the area (or neither) */
		
		boolean contains;
		boolean intersects;
		
		{
			final LineSegmentXZ segment = line.getLineSegment();
			final PolygonWithHolesXZ polygon = area.getPolygon();
			
			if (!line.isConnectedTo(area)) {
	
				intersects = polygon.intersects(segment);
				contains = !intersects && polygon.contains(segment);
				
			} else {
			
				/* check whether the line intersects the area somewhere
				 * else than just at the common node(s).
				 */
				
				intersects = false;
			
				double segmentLength = distance(segment.p1, segment.p2);
				
				for (VectorXZ pos : polygon.intersectionPositions(segment)) {
					if (distance(pos, segment.p1) > segmentLength / 100
							&& distance(pos, segment.p2) > segmentLength / 100) {
						intersects = true;
						break;
					}
				}
	
				/* check whether the area contains the line's center.
				 * Unless the line intersects the area outline,
				 * this means that the area contains the line itself.
				 */
							
				contains = !intersects && polygon.contains(segment.getCenter());
				
			}
			
		}
		
		/* add an overlap if detected */
					
		if (contains || intersects) {
			
			/* add the overlap */
			
			MapOverlapWA newOverlap = new MapOverlapWA(line, area,
						intersects ? MapOverlapType.INTERSECT : MapOverlapType.CONTAIN);
			
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
		
		boolean contains;
		boolean intersects;
		
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
			
			intersects = false;
			
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
			
			contains = polygon1.contains(polygon2.getOuter())
				|| polygon2.contains(polygon2.getOuter());
									
		}
		
		/* add an overlap if detected */
					
		if (contains || intersects) {
			
			/* add the overlap */
			
			MapOverlapAA newOverlap = new MapOverlapAA(area1, area2,
				intersects ? MapOverlapType.INTERSECT : MapOverlapType.CONTAIN);
			
			area1.addOverlap(newOverlap);
			area2.addOverlap(newOverlap);
			
		}
		
	}

	private AxisAlignedBoundingBoxXZ calculateFileBoundary(
			Collection<Bound> bounds) {
		
		Collection<VectorXZ> boundedPoints = new ArrayList<VectorXZ>();
		
		for (Bound bound : bounds) {
			
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
