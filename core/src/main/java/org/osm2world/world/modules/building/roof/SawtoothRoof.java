package org.osm2world.world.modules.building.roof;

import static java.lang.Math.*;
import static java.util.Objects.requireNonNullElse;
import static org.osm2world.util.ValueParseUtil.parseMeasure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.Angle;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.GeometryUtil;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

public class SawtoothRoof extends RoofWithInnerLines {

	private final double UPSLOPE_PROPORTION = 0.05;

	private final int numTeeth;
	private final double toothLength;
	private final List<LineSegmentXZ> ridges;
	private final List<LineSegmentXZ> lowerEdges;

	public SawtoothRoof(@Nullable BuildingPart buildingPart, PolygonWithHolesXZ originalPolygon, TagSet tags,
			Material material) {

		super(buildingPart, originalPolygon, tags, material);

		SimplePolygonXZ simplifiedPolygon = originalPolygon.getOuter().getSimplifiedPolygon();

		VectorXZ ridgeDirection = RoofWithRidge.ridgeDirectionFromTags(tags, false, simplifiedPolygon, null);

		/* get a bbox around the roof (segments will be orthogonal to the ridge and cover this bbox) */

		Angle ridgeAngle = Angle.ofRadians(ridgeDirection.angle());
		var bbox = this.originalPolygon.getOuter().rotatedBoundingBox(ridgeAngle);
		List<LineSegmentXZ> bboxSegments = bbox.getSegments();

		var leftSide = bboxSegments.get(3).reverse();
		var rightSide = bboxSegments.get(1);

		/* determine the number of teeth */

		double length = leftSide.getLength();
		double height = requireNonNullElse(calculatePreliminaryHeight(), BuildingPart.DEFAULT_RIDGE_HEIGHT);

		Angle roofAngle = parseRoofAngle(tags);
		roofAngle = requireNonNullElse(roofAngle, Angle.ofDegrees(45));

		double calculatedToothLength = height / tan(roofAngle.radians);
		numTeeth = max(2, (int) Math.round(length / calculatedToothLength));
		this.toothLength = length / numTeeth;

		/* draw lines between the left and right side which split the roof */

		ridges = new ArrayList<>(numTeeth);
		lowerEdges = new ArrayList<>(numTeeth - 1);

		for (int i = 0; i < numTeeth; i++) {

			ridges.add(new LineSegmentXZ(
					leftSide.pointAtOffset(i * toothLength),
					rightSide.pointAtOffset(i * toothLength)));

			if (i > 0) {
				lowerEdges.add(new LineSegmentXZ(
						leftSide.pointAtOffset((i - UPSLOPE_PROPORTION) * toothLength),
						rightSide.pointAtOffset((i - UPSLOPE_PROPORTION) * toothLength)));
			}

		}

	}

	@Override
	protected Collection<InnerLine> getInnerLines() {

		List<InnerLine> innerLines = new ArrayList<>();

		ridges.subList(1, ridges.size()).forEach(it -> innerLines.add(new InnerLine(it, true)));
		lowerEdges.forEach(it -> innerLines.add(new InnerLine(it, true)));

		return innerLines;

	}

	@Override
	protected Double getRoofHeightAt_noInterpolation(VectorXZ pos) {

		double distanceFromFirstRidge = GeometryUtil.distanceFromLine(pos, ridges.get(0).p1,  ridges.get(0).p2);

		int onTooth = min(numTeeth - 1, (int) floor(distanceFromFirstRidge / toothLength));
		boolean lastTooth = onTooth == numTeeth - 1;

		double distanceAlongTooth = distanceFromFirstRidge - onTooth * toothLength;
		double relativeDistanceAlongTooth = distanceAlongTooth / toothLength;

		double downslopeProportion = lastTooth ? 1.0 : 1 - UPSLOPE_PROPORTION;
		if (relativeDistanceAlongTooth < downslopeProportion) {
			return roofHeight * max(lastTooth ? 0 : 0.05, (1 - (relativeDistanceAlongTooth / (downslopeProportion))));
		} else {
			return roofHeight * max(lastTooth ? 0 : 0.05, (relativeDistanceAlongTooth - (downslopeProportion)) / UPSLOPE_PROPORTION);
		}

	}

	@Override
	public Double calculatePreliminaryHeight() {
		return parseMeasure(tags.getValue("roof:height"));
	}

}
