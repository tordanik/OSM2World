package org.osm2world.world.modules.building.roof;

import static org.osm2world.util.ValueParseUtil.parseMeasure;

import java.util.*;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapWaySegment;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.map_data.data.overlaps.MapOverlap;
import org.osm2world.map_data.data.overlaps.MapOverlapWA;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.GeometryUtil;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

/**
 * roof that has been mapped with explicit roof edge/ridge/apex elements
 */
public class ComplexRoof extends RoofWithInnerLines {

	private final Collection<LineSegmentXZ> ridgeAndEdgeSegments;
	private final List<MapWaySegment> edges;
	private final List<MapWaySegment> ridges;

	private Map<VectorXZ, Double> roofHeightMap = null;

	public ComplexRoof(@Nullable BuildingPart buildingPart, MapArea area, PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {

		super(buildingPart, originalPolygon, tags, material);

		/* find ridge and/or edges
		 * (apex nodes don't need to be handled separately
		 *  as they should always be part of an edge segment) */

		ridgeAndEdgeSegments = new ArrayList<>();

		List<MapNode> nodes = area.getBoundaryNodes();

		edges = new ArrayList<>();
		ridges = new ArrayList<>();

		for (MapOverlap<?,?> overlap : area.getOverlaps()) {

			if (overlap instanceof MapOverlapWA overlapWA) {

				MapWaySegment waySegment = overlapWA.e1;

				boolean isRidge = waySegment.getTags().contains("roof:ridge", "yes");
				boolean isEdge = waySegment.getTags().contains("roof:edge", "yes");

				if (!(isRidge || isEdge))
					continue;

				boolean inside = originalPolygon.contains(waySegment.getCenter());
				boolean intersects = originalPolygon.intersects(waySegment.getLineSegment());

				// check also endpoints as pnpoly algo is not reliable when
				// segment lies on the polygon edge
				boolean containsStart = nodes.contains(waySegment.getStartNode());
				boolean containsEnd = nodes.contains(waySegment.getEndNode());

				if (!inside && !intersects && !(containsStart && containsEnd))
					continue;

				if (isEdge) {
					edges.add(waySegment);
				} else {
					ridges.add(waySegment);
				}

				ridgeAndEdgeSegments.add(waySegment.getLineSegment());
			}
		}

	}

	@Override
	protected Collection<InnerLine> getInnerLines() {
		return ridgeAndEdgeSegments.stream().map(InnerLine::new).toList();
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {
		if (roofHeightMap == null) {
			calculateRoofHeightMap();
		}
		return roofHeightMap.getOrDefault(pos, null);
	}

	private void calculateRoofHeightMap() {

		roofHeightMap = new HashMap<>();

		CalculationResults calculationResults = super.calculatePolygonAndInnerSegments();

		for (MapWaySegment waySegment : edges) {
			for (MapNode node : waySegment.getStartEndNodes()) {

				// height of node (above roof base)
				Double nodeHeight = null;

				if (node.getTags().containsKey("roof:height")) {
					nodeHeight = parseMeasure(node.getTags().getValue("roof:height"));
					// hmm, shouldn't edges be interpolated? some seem to think they don't
				} else if (waySegment.getTags().containsKey("roof:height")) {
					nodeHeight = parseMeasure(waySegment.getTags().getValue("roof:height"));
				} else if (node.getTags().contains("roof:apex",	"yes")) {
					nodeHeight = roofHeight();
				}

				if (nodeHeight != null) {
					roofHeightMap.put(node.getPos(), nodeHeight);
				}

			}
		}

		for (MapWaySegment waySegment : ridges) {

			// height of node (above roof base)
			Double nodeHeight;

			if (waySegment.getTags().containsKey("roof:height")) {
				nodeHeight = parseMeasure(waySegment.getTags().getValue("roof:height"));
			} else {
				nodeHeight = roofHeight();
			}

			for (MapNode node : waySegment.getStartEndNodes()) {
				roofHeightMap.put(node.getPos(), nodeHeight);
			}

		}

		/* add heights for intersection points which aren't nodes */

		Map<VectorXZ, InnerLine> extraPoints = calculationResults.extraPoints();

		for (VectorXZ v : extraPoints.keySet()) {
			LineSegmentXZ segment = extraPoints.get(v).segment();
			double h0 = roofHeightMap.get(segment.p1);
			double h1 = roofHeightMap.get(segment.p2);
			if (h0 == h1) {
				roofHeightMap.put(v, h0);
			} else {
				roofHeightMap.put(v, GeometryUtil.interpolateValue(v, segment.p1, h0, segment.p2, h1));
			}
		}

		/* add heights for outline nodes that don't have one yet */

		for (var ring : calculationResults.polygon().getRings()) {
			for (VectorXZ v : ring.getVertices()) {
				if (!roofHeightMap.containsKey(v)) {
					roofHeightMap.put(v, 0.0);
				}
			}
		}

		/* add heights for edge nodes that are not also
		 * ridge/outline/apex nodes. This will just use base height
		 * for them instead of trying to interpolate heights along
		 * chains of edge segments. Results are therefore wrong,
		 * but there's no reason to map them like that anyway. */

		for (LineSegmentXZ segment : ridgeAndEdgeSegments) {
			if (!roofHeightMap.containsKey(segment.p1)) {
				roofHeightMap.put(segment.p1, 0.0);
			}
			if (!roofHeightMap.containsKey(segment.p2)) {
				roofHeightMap.put(segment.p2, 0.0);
			}
		}

	}

	public static boolean hasComplexRoof(MapArea area) {
		for (MapOverlap<?,?> overlap : area.getOverlaps()) {
			if (overlap instanceof MapOverlapWA) {
				TagSet tags = overlap.e1.getTags();
				if (tags.contains("roof:ridge", "yes") || tags.contains("roof:edge", "yes")) {
					return true;
				}
			}
		}
		return false;
	}

}