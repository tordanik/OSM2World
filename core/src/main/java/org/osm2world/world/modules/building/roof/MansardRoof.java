package org.osm2world.world.modules.building.roof;

import static org.osm2world.math.algorithms.GeometryUtil.interpolateBetween;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

public class MansardRoof extends RoofWithRidge {

	private final LineSegmentXZ mansardEdge1, mansardEdge2;

	public MansardRoof(@Nullable BuildingPart buildingPart, PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {

		super(buildingPart, 1/3.0, originalPolygon, tags, material);

		mansardEdge1 = new LineSegmentXZ(
				interpolateBetween(cap1.p1, ridge.p1, 1/3.0),
				interpolateBetween(cap2.p1, ridge.p2, 1/3.0));

		mansardEdge2 = new LineSegmentXZ(
				interpolateBetween(cap1.p2, ridge.p1, 1/3.0),
				interpolateBetween(cap2.p2, ridge.p2, 1/3.0));

	}

	@Override
	protected Collection<InnerLine> getInnerLines() {
		List<LineSegmentXZ> segments = List.of(ridge,
				mansardEdge1,
				mansardEdge2,
				new LineSegmentXZ(ridge.p1, mansardEdge1.p1),
				new LineSegmentXZ(ridge.p1, mansardEdge2.p1),
				new LineSegmentXZ(ridge.p2, mansardEdge1.p2),
				new LineSegmentXZ(ridge.p2, mansardEdge2.p2),
				new LineSegmentXZ(cap1.p1, mansardEdge1.p1),
				new LineSegmentXZ(cap2.p1, mansardEdge1.p2),
				new LineSegmentXZ(cap1.p2, mansardEdge2.p1),
				new LineSegmentXZ(cap2.p2, mansardEdge2.p2),
				new LineSegmentXZ(mansardEdge1.p1, mansardEdge2.p1),
				new LineSegmentXZ(mansardEdge1.p2, mansardEdge2.p2));
		return segments.stream().map(InnerLine::new).toList();
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {

		if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) {
			return roofHeight();
		} else if (getPolygon().getOuter().getVertexCollection().contains(pos)) {
			return 0.0;
		} else if (mansardEdge1.p1.equals(pos)
				|| mansardEdge1.p2.equals(pos)
				|| mansardEdge2.p1.equals(pos)
				|| mansardEdge2.p2.equals(pos)) {
			return roofHeight() - 1/3.0 * roofHeight();
		} else {
			return null;
		}

	}

}