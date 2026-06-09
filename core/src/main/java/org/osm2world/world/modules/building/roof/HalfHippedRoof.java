package org.osm2world.world.modules.building.roof;

import static java.util.Collections.emptyList;
import static org.osm2world.math.algorithms.GeometryUtil.*;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

public class HalfHippedRoof extends RoofWithRidge {

	private final LineSegmentXZ cap1part, cap2part;

	public HalfHippedRoof(@Nullable BuildingPart buildingPart, PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {

		super(buildingPart, 1/6.0, originalPolygon, tags, material);

		cap1part = new LineSegmentXZ(
				interpolateBetween(cap1.p1, cap1.p2, 0.5 - ridgeOffset / cap1.getLength()),
				interpolateBetween(cap1.p1, cap1.p2, 0.5 + ridgeOffset / cap1.getLength()));

		cap2part = new LineSegmentXZ(
				interpolateBetween(cap2.p1, cap2.p2, 0.5 - ridgeOffset / cap1.getLength()),
				interpolateBetween(cap2.p1, cap2.p2, 0.5 + ridgeOffset / cap1.getLength()));

	}

	@Override
	public Collection<VectorXZ> getInnerPoints() {
		return emptyList();
	}

	@Override
	protected Collection<InnerLine> getInnerLines() {
		return List.of(
				new InnerLine(ridge),
				new InnerLine(new LineSegmentXZ(ridge.p1, cap1part.p1), null, false, true),
				new InnerLine(new LineSegmentXZ(ridge.p1, cap1part.p2), null, false, true),
				new InnerLine(new LineSegmentXZ(ridge.p2, cap2part.p1), null, false, true),
				new InnerLine(new LineSegmentXZ(ridge.p2, cap2part.p2), null, false, true));
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {
		if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) { // point on the ridge
			return roofHeight;
		} else if (distanceFromLineSegment(pos, cap1part) < 0.05) { // point ~on cap1part
			return roofHeight - roofHeight * ridgeOffset / (cap1.getLength()/2);
		} else if (distanceFromLineSegment(pos, cap2part) < 0.05) { // point ~on cap2part
			return roofHeight - roofHeight * ridgeOffset / (cap2.getLength()/2);
		} else if (distanceFromLineSegment(pos, cap1) < 0.05) { // point ~on cap1
			double relativeRidgeDist = distanceFromLine(pos, ridge.p1, ridge.p2) / (cap1.getLength() / 2);
			return roofHeight * (1 - relativeRidgeDist);
		} else if (distanceFromLineSegment(pos, cap2) < 0.05) { // point ~on cap2
			double relativeRidgeDist = distanceFromLine(pos, ridge.p1, ridge.p2) / (cap2.getLength() / 2);
			return roofHeight * (1 - relativeRidgeDist);
		} else if (getPolygon().getOuter().getVertexCollection().contains(pos)) { // other points on the outline
			return 0.0;
		} else {
			return null;
		}
	}

}