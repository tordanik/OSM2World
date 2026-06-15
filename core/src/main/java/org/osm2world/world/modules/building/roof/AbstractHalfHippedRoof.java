package org.osm2world.world.modules.building.roof;

import static java.lang.Math.max;
import static org.osm2world.math.algorithms.GeometryUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

public abstract class AbstractHalfHippedRoof extends RoofWithRidge {

	private final LineSegmentXZ cap1part, cap2part;

	public AbstractHalfHippedRoof(@Nullable BuildingPart buildingPart, double relativeRidgeOffset1,
			double relativeRidgeOffset2, boolean ridgeAlongDirection, PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {

		super(buildingPart, relativeRidgeOffset1, relativeRidgeOffset2, ridgeAlongDirection, null, originalPolygon, tags, material);

		cap1part = new LineSegmentXZ(
				interpolateBetween(cap1.p1, cap1.p2, 0.5 - ridgeOffset1 / cap1.getLength()),
				interpolateBetween(cap1.p1, cap1.p2, 0.5 + ridgeOffset1 / cap1.getLength()));

		cap2part = new LineSegmentXZ(
				interpolateBetween(cap2.p1, cap2.p2, 0.5 - ridgeOffset2 / cap1.getLength()),
				interpolateBetween(cap2.p1, cap2.p2, 0.5 + ridgeOffset2 / cap1.getLength()));

	}

	@Override
	protected Collection<InnerLine> getInnerLines() {
		List<InnerLine> innerLines = new ArrayList<>(5);
		innerLines.add(new InnerLine(ridge, ridgeOffset1 == 0, ridgeOffset2 == 0));
		if (ridgeOffset1 > 0) {
			innerLines.add(new InnerLine(new LineSegmentXZ(ridge.p1, cap1part.p1), false, true));
			innerLines.add(new InnerLine(new LineSegmentXZ(ridge.p1, cap1part.p2), false, true));
		}
		if (ridgeOffset2 > 0) {
			innerLines.add(new InnerLine(new LineSegmentXZ(ridge.p2, cap2part.p1), false, true));
			innerLines.add(new InnerLine(new LineSegmentXZ(ridge.p2, cap2part.p2), false, true));
		}
		return innerLines;
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {
		double roofHeight = roofHeight();
		if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) { // point on the ridge
			return roofHeight;
		} else if (distanceFromLineSegment(pos, cap1part) < 0.05) { // point ~on cap1part
			return roofHeight - roofHeight * ridgeOffset1 / (cap1.getLength()/2);
		} else if (distanceFromLineSegment(pos, cap2part) < 0.05) { // point ~on cap2part
			return roofHeight - roofHeight * ridgeOffset2 / (cap2.getLength()/2);
		} else if (distanceFromLineSegment(pos, cap1) < 0.05) { // point ~on cap1
			double relativeRidgeDist = distanceFromLine(pos, ridge.p1, ridge.p2) / (cap1.getLength() / 2);
			return max(roofHeight * (1 - relativeRidgeDist), 0.0);
		} else if (distanceFromLineSegment(pos, cap2) < 0.05) { // point ~on cap2
			double relativeRidgeDist = distanceFromLine(pos, ridge.p1, ridge.p2) / (cap2.getLength() / 2);
			return max(roofHeight * (1 - relativeRidgeDist), 0.0);
		} else if (getPolygon().getOuter().getVertexCollection().contains(pos)) { // other points on the outline
			return 0.0;
		} else {
			return null;
		}
	}

}