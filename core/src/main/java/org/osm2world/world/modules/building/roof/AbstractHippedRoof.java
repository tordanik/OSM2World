package org.osm2world.world.modules.building.roof;

import static org.osm2world.math.algorithms.GeometryUtil.distanceFromLine;
import static org.osm2world.math.algorithms.GeometryUtil.isRightOf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

/**
 * Shared superclass of {@link HippedRoof} and {@link SideHippedRoof}
 */
public abstract class AbstractHippedRoof extends RoofWithRidge {

	private final double maxOrthogonalDistanceToRidgeLine;

	public AbstractHippedRoof(@Nullable BuildingPart buildingPart, double relativeRidgeOffset1,
			double relativeRidgeOffset2, boolean ridgeAlongDirection, PolygonWithHolesXZ originalPolygon, TagSet tags,
			Material material) {

		super(buildingPart, relativeRidgeOffset1, relativeRidgeOffset2, ridgeAlongDirection, null,
				originalPolygon, tags, material);

		maxOrthogonalDistanceToRidgeLine = originalPolygon.getOuter().getVertices().stream()
				.mapToDouble(v -> distanceFromLine(v, ridge.p1, ridge.p2))
				.max().getAsDouble();

	}

	@Override
	protected Collection<InnerLine> getInnerLines() {
		List<InnerLine> innerLines = new ArrayList<>(5);
		innerLines.add(new InnerLine(ridge, ridgeOffset1 == 0, ridgeOffset2 == 0));
		if (ridgeOffset1 > 0) {
			innerLines.add(new InnerLine(new LineSegmentXZ(ridge.p1, cap1.p1)));
			innerLines.add(new InnerLine(new LineSegmentXZ(ridge.p1, cap1.p2)));
		}
		if (ridgeOffset2 > 0) {
			innerLines.add(new InnerLine(new LineSegmentXZ(ridge.p2, cap2.p1)));
			innerLines.add(new InnerLine(new LineSegmentXZ(ridge.p2, cap2.p2)));
		}
		return innerLines;
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {

		double roofHeight = roofHeight();

		// The logic which would be used for a gabled roof.
		// Can be used here as well, except for the triangular segments at both ends.
		Function<VectorXZ, VectorXYZ> heightFromRidgeDistance = (VectorXZ p) -> {
			double distRidge = distanceFromLine(p, ridge.p1, ridge.p2);
			double relativePlacement = distRidge / maxOrthogonalDistanceToRidgeLine;
			double height = roofHeight - roofHeight * relativePlacement;
			return p.xyz(height);
		};


		if (ridgeOffset1 > 0 && isRightOf(pos, ridge.p1, cap1.p2) && !isRightOf(pos, ridge.p1, cap1.p1)) {

			// pos is in the triangular sector at p1 of the ridge

			var triangle = new TriangleXYZ(
					ridge.p1.xyz(roofHeight),
					heightFromRidgeDistance.apply(cap1.p1),
					heightFromRidgeDistance.apply(cap1.p2));

			return triangle.getYAt(pos);

		} else if (ridgeOffset1 > 0 && isRightOf(pos, ridge.p2, cap2.p1) && !isRightOf(pos, ridge.p2, cap2.p2)) {

			// pos is in the triangular sector at p1 of the ridge

			var triangle = new TriangleXYZ(
					ridge.p1.xyz(roofHeight),
					heightFromRidgeDistance.apply(cap2.p1),
					heightFromRidgeDistance.apply(cap2.p2));

			return triangle.getYAt(pos);

		}

		return heightFromRidgeDistance.apply(pos).y;

	}

}