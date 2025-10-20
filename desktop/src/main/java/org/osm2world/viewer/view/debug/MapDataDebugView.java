package org.osm2world.viewer.view.debug;

import static java.util.Collections.emptyList;
import static org.osm2world.map_data.creation.EmptyTerrainBuilder.EMPTY_SURFACE_VALUE;

import java.util.Collection;
import java.util.function.Predicate;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapWaySegment;
import org.osm2world.map_data.data.overlaps.MapIntersectionWW;
import org.osm2world.map_data.data.overlaps.MapOverlap;
import org.osm2world.map_data.data.overlaps.MapOverlapAA;
import org.osm2world.map_data.data.overlaps.MapOverlapWA;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.TriangulationUtil;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.TriangleXZ;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.ImmutableMaterial;
import org.osm2world.scene.material.Material.Interpolation;

/**
 * shows the plain {@link MapData} as a network of nodes, lines and areas
 */
public class MapDataDebugView extends StaticDebugView {

	private static final Color LINE_COLOR = Color.WHITE;
	private static final Color NODE_COLOR = Color.YELLOW;
	private static final Color INTERSECTION_COLOR = Color.RED;
	private static final Color SHARED_SEGMENT_COLOR = Color.ORANGE;
	private static final Color AREA_COLOR = new Color(0.8f, 0.8f, 1);

	private static final float HALF_NODE_WIDTH = 0.4f;

	public MapDataDebugView() {
		super("Map data", "shows the raw map data as a network of nodes, lines and areas");
	}

	@Override
	public void fillOutput(JOGLOutput output) {

		Predicate<MapArea> isEmptyTerrain = it -> it.getTags().contains("surface", EMPTY_SURFACE_VALUE);
		MapData mapData = scene.getMapData();

		for (MapArea area : mapData.getMapAreas()) {

			if (isEmptyTerrain.test(area)) continue;

			Collection<TriangleXZ> triangles = TriangulationUtil.triangulate(area.getPolygon());

			output.drawTriangles(
					new ImmutableMaterial(Interpolation.FLAT, AREA_COLOR),
					triangles.stream().map(t -> t.xyz(-0.1)).toList(),
					emptyList());

		}

		for (MapWaySegment line : mapData.getMapWaySegments()) {
			drawArrow(output, LINE_COLOR, 0.7f,
					line.getStartNode().getPos().xyz(0),
					line.getEndNode().getPos().xyz(0));
		}

		for (MapNode node : mapData.getMapNodes()) {
			if (node.getId() < 0 && node.getAdjacentAreas().stream().allMatch(isEmptyTerrain)) continue;
			drawBoxAround(output, node.getPos(), NODE_COLOR, HALF_NODE_WIDTH);
		}

		for (MapWaySegment line : mapData.getMapWaySegments()) {
			for (MapIntersectionWW intersection : line.getIntersectionsWW()) {
				drawBoxAround(output, intersection.pos,
						INTERSECTION_COLOR, HALF_NODE_WIDTH);
			}
		}

		for (MapArea area : mapData.getMapAreas()) {
			if (isEmptyTerrain.test(area)) continue;
			for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
				if (overlap instanceof MapOverlapWA overlapWA) {
					for (VectorXZ pos : overlapWA.getIntersectionPositions()) {
						drawBoxAround(output, pos, INTERSECTION_COLOR, HALF_NODE_WIDTH);
					}
					for (LineSegmentXZ seg : overlapWA.getSharedSegments()) {
						output.drawLineStrip(SHARED_SEGMENT_COLOR, 3, seg.p1.xyz(0), seg.p2.xyz(0));
					}
					for (LineSegmentXZ seg : overlapWA.getOverlappedSegments()) {
						output.drawLineStrip(INTERSECTION_COLOR, 3, seg.p1.xyz(0), seg.p2.xyz(0));
					}
				} else if (overlap instanceof MapOverlapAA overlapAA) {
					if (isEmptyTerrain.test(overlapAA.getOther(area))) continue;
					for (VectorXZ pos : overlapAA.getIntersectionPositions()) {
						drawBoxAround(output, pos, INTERSECTION_COLOR, HALF_NODE_WIDTH);
					}
				}
			}
		}

	}

}
