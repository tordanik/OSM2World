package org.osm2world.world.modules.building.roof;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

public class SideHippedRoof extends AbstractHippedRoof {

	public SideHippedRoof(@Nullable BuildingPart buildingPart, PolygonWithHolesXZ originalPolygon, TagSet tags,
			Material material) {
		super(buildingPart, 0, HippedRoof.RIDGE_OFFSET, true, originalPolygon, tags, material);
	}

	@Override
	protected @Nullable VectorXZ defaultDirection() {
		if (buildingPart == null) return null;
		return determineDefaultDirection(buildingPart.getPrimaryMapElement());
	}

	static @Nullable VectorXZ determineDefaultDirection(MapArea area) {

		Collection<LineSegmentXZ> connectedSegments = outlineSegmentsWithAdjacentBuildingParts(area);

		if (connectedSegments.isEmpty()) return null;

		if (!area.getOuterPolygon().isClockwise()) {
			connectedSegments = connectedSegments.stream().map(LineSegmentXZ::reverse).toList();
		}

		VectorXZ segmentDirection = connectedSegments.iterator().next().getDirection();

		for (LineSegmentXZ otherSegment : connectedSegments) {
			if (VectorXZ.angleBetween(segmentDirection, otherSegment.getDirection()) > 0.1) {
				// multiple connected segments with contradictory direction
				return null;
			}
		}

		return segmentDirection.rightNormal();

	}

	private static Collection<LineSegmentXZ> outlineSegmentsWithAdjacentBuildingParts(MapArea area) {

		Collection<LineSegmentXZ> result = new ArrayList<>();

		Predicate<MapNode> touchesOtherPart = (MapNode n) -> n.getAdjacentAreas().stream().anyMatch(
				a -> a != area && a.getTags().containsAny(List.of("building", "building:part"), null));

		List<MapNode> nodes = area.getBoundaryNodes();

		for (int i = 0; i < nodes.size() - 1 /* duplicated first node */; i++) {
			MapNode nodeA = nodes.get(i);
			MapNode nodeB = nodes.get(i + 1);
			if (touchesOtherPart.test(nodeA) && touchesOtherPart.test(nodeB)) {
				result.add(new LineSegmentXZ(nodeA.getPos(), nodeB.getPos()));
			}
		}

		return result;

	}

}
