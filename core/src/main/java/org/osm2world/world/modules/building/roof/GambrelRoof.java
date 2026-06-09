package org.osm2world.world.modules.building.roof;

import static org.osm2world.math.algorithms.GeometryUtil.distanceFromLineSegment;
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

public class GambrelRoof extends RoofWithRidge {

	private final LineSegmentXZ cap1part, cap2part;

	public GambrelRoof(@Nullable BuildingPart buildingPart, PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {

		super(buildingPart, 0, originalPolygon, tags, material);

		cap1part = new LineSegmentXZ(
				interpolateBetween(cap1.p1, cap1.p2, 1/6.0),
				interpolateBetween(cap1.p1, cap1.p2, 5/6.0));

		cap2part = new LineSegmentXZ(
				interpolateBetween(cap2.p1, cap2.p2, 1/6.0),
				interpolateBetween(cap2.p1, cap2.p2, 5/6.0));

	}

	@Override
	protected Collection<InnerLine> getInnerLines() {
		return List.of(
				new InnerLine(ridge, true),
				new InnerLine(new LineSegmentXZ(cap1part.p1, cap2part.p2), true),
				new InnerLine(new LineSegmentXZ(cap1part.p2, cap2part.p1), true));
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {

		double distRidge = distanceFromLineSegment(pos, ridge);
		double relativePlacement = distRidge / maxDistanceToRidge;

		if (relativePlacement < 2/3.0) {
			return roofHeight - 1/2.0 * roofHeight * relativePlacement;
		} else {
			return roofHeight - 1/3.0 * roofHeight - 2 * roofHeight * (relativePlacement - 2/3.0);
		}

	}

}