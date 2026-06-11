package org.osm2world.world.modules.building.roof;

import static org.osm2world.math.algorithms.GeometryUtil.distanceFromLineSegment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

/**
 * Shared superclass of {@link HippedRoof} and {@link SideHippedRoof}
 */
public abstract class AbstractHippedRoof extends RoofWithRidge {

	public AbstractHippedRoof(@Nullable BuildingPart buildingPart, double relativeRidgeOffset1,
			double relativeRidgeOffset2, boolean ridgeAlongDirection, PolygonWithHolesXZ originalPolygon, TagSet tags,
			Material material) {
		super(buildingPart, relativeRidgeOffset1, relativeRidgeOffset2, ridgeAlongDirection, originalPolygon, tags, material);
	}

	@Override
	protected Collection<InnerLine> getInnerLines() {
		List<InnerLine> innerLines = new ArrayList<>(5);
		innerLines.add(new InnerLine(ridge, null, ridgeOffset1 == 0, ridgeOffset2 == 0));
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
		double distRidge = distanceFromLineSegment(pos, ridge);
		double relativePlacement = distRidge / maxDistanceToRidge;
		return roofHeight - roofHeight * relativePlacement;
	}

}