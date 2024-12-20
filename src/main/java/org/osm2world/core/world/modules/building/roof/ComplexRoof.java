package org.osm2world.core.world.modules.building.roof;

import static java.util.Collections.emptyList;
import static org.osm2world.core.math.GeometryUtil.distanceFromLineSegment;
import static org.osm2world.core.util.ValueParseUtil.parseMeasure;

import java.util.*;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_data.data.overlaps.MapOverlapWA;
import org.osm2world.core.math.*;
import org.osm2world.core.target.common.material.Material;

/**
 * roof that has been mapped with explicit roof edge/ridge/apex elements
 */
public class ComplexRoof extends HeightfieldRoof {

	private final Collection<LineSegmentXZ> ridgeAndEdgeSegments;
	private final List<MapWaySegment> edges;
	private final List<MapWaySegment> ridges;

	private Map<VectorXZ, Double> roofHeightMap;
	private PolygonWithHolesXZ simplePolygon;

	public ComplexRoof(MapArea area, PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {

		super(originalPolygon, tags, material);

		/* find ridge and/or edges
		 * (apex nodes don't need to be handled separately
		 *  as they should always be part of an edge segment) */

		ridgeAndEdgeSegments = new ArrayList<>();

		List<MapNode> nodes = area.getBoundaryNodes();

		edges = new ArrayList<>();
		ridges = new ArrayList<>();

		for (MapOverlap<?,?> overlap : area.getOverlaps()) {

			if (overlap instanceof MapOverlapWA) {

				MapWaySegment waySegment = ((MapOverlapWA) overlap).e1;

				boolean isRidge = waySegment.getTags().contains("roof:ridge", "yes");
				boolean isEdge = waySegment.getTags().contains("roof:edge", "yes");

				if (!(isRidge || isEdge))
					continue;

				boolean inside = originalPolygon.contains(waySegment.getCenter());

				// check also endpoints as pnpoly algo is not reliable when
				// segment lies on the polygon edge
				boolean containsStart = nodes.contains(waySegment.getStartNode());
				boolean containsEnd = nodes.contains(waySegment.getEndNode());

				if (!inside && !(containsStart && containsEnd))
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
	public PolygonWithHolesXZ getPolygon() {
		calculateRoofHeightMap();
		return simplePolygon;
	}

	@Override
	public Collection<VectorXZ> getInnerPoints() {
		return emptyList();
	}

	@Override
	public Collection<LineSegmentXZ> getInnerSegments() {
		return ridgeAndEdgeSegments;
	}


	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {
		calculateRoofHeightMap();
		if (roofHeightMap.containsKey(pos)) {
			return roofHeightMap.get(pos);
		} else {
			return null;
		}
	}

	private void calculateRoofHeightMap() {

		if (roofHeight == null) throw new IllegalStateException("Roof height not set yet");

		roofHeightMap = new HashMap<>();

		Set<VectorXZ> nodeSet = new HashSet<>();

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
					nodeHeight = roofHeight;
				}

				if (nodeHeight == null) {
					nodeSet.add(node.getPos());
					continue;
				}

				roofHeightMap.put(node.getPos(), (double) nodeHeight);

			}
		}

		for (MapWaySegment waySegment : ridges) {

			// height of node (above roof base)
			Double nodeHeight = null;

			if (waySegment.getTags().containsKey("roof:height")) {
				nodeHeight = parseMeasure(waySegment.getTags().getValue("roof:height"));
			} else {
				nodeHeight = roofHeight;
			}

			for (MapNode node : waySegment.getStartEndNodes()) {
				roofHeightMap.put(node.getPos(), (double) nodeHeight);
			}

		}

		/* join colinear segments, but not the nodes that are connected to ridge/edges
		 * often there are nodes that are only added to join one building to another
		 * but these interfere with proper triangulation.
		 * TODO: do the same for holes */
		List<VectorXZ> vertices = originalPolygon.getOuter().vertices();
		List<VectorXZ> simplified = new ArrayList<>();
		VectorXZ vPrev = vertices.get(vertices.size() - 2);

		for (int i = 0, size = vertices.size() - 1; i < size; i++) {
			VectorXZ v = vertices.get(i);

			if (i == 0 || roofHeightMap.containsKey(v) || nodeSet.contains(v)) {
				simplified.add(v);
				vPrev = v;
				continue;
			}
			VectorXZ vNext = vertices.get(i + 1);
			LineSegmentXZ l = new LineSegmentXZ(vPrev, vNext);

			// TODO define as static somewhere: 10 cm tolerance
			if (distanceFromLineSegment(v, l) < 0.01){
				continue;
			}

			roofHeightMap.put(v, 0.0);
			simplified.add(v);
			vPrev = v;
		}

		if (simplified.size() > 2) {
			try {
				simplified.add(simplified.get(0));
				simplePolygon = new PolygonWithHolesXZ(new SimplePolygonXZ(simplified), originalPolygon.getHoles());
			} catch (InvalidGeometryException e) {
				System.err.print(e.getMessage());
				simplePolygon = originalPolygon;
			}
		} else {
			simplePolygon = originalPolygon;
		}

		/* add heights for outline nodes that don't have one yet */

		for (VectorXZ v : simplePolygon.getOuter().getVertices()) {
			if (!roofHeightMap.containsKey(v)) {
				roofHeightMap.put(v, 0.0);
			}
		}

		for (SimplePolygonXZ hole : simplePolygon.getHoles()) {
			for (VectorXZ v : hole.getVertices()) {
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

	public static final boolean hasComplexRoof(MapArea area) {
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