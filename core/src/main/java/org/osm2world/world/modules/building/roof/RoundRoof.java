package org.osm2world.world.modules.building.roof;

import static java.lang.Math.*;
import static java.util.Collections.emptyList;
import static org.osm2world.math.algorithms.GeometryUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

public class RoundRoof extends RoofWithRidge {

	private final static double ROOF_SUBDIVISIONS_PER_HEIGHT_METER = 1;

	private final List<LineSegmentXZ> capParts;
	private final int rings;

	public RoundRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {

		super(0, originalPolygon, tags, material.makeSmooth());

		/* determine how many segments to use to simulate a round surface */

		double height = Objects.requireNonNullElse(super.calculatePreliminaryHeight(), BuildingPart.DEFAULT_RIDGE_HEIGHT);
		rings = (int)max(3, height / ROOF_SUBDIVISIONS_PER_HEIGHT_METER);

		/* decide where to place the segments */

		capParts = new ArrayList<>(rings*2);
		float step = 0.5f / (rings + 1);
		for (int i = 1; i <= rings; i++) {
			capParts.add(new LineSegmentXZ(
					interpolateBetween(cap1.p1, cap1.p2, i * step),
					interpolateBetween(cap1.p1, cap1.p2, 1 - i * step)));

			capParts.add(new LineSegmentXZ(
					interpolateBetween(cap2.p1, cap2.p2, i * step),
					interpolateBetween(cap2.p1, cap2.p2, 1 - i * step)));
		}
	}

	@Override
	public PolygonWithHolesXZ getPolygon() {

		SimplePolygonXZ newOuter = originalPolygon.getOuter();

		newOuter = insertIntoPolygon(newOuter, ridge.p1, SNAP_DISTANCE);
		newOuter = insertIntoPolygon(newOuter, ridge.p2, SNAP_DISTANCE);

		for (LineSegmentXZ capPart : capParts){
			newOuter = insertIntoPolygon(newOuter, capPart.p1, SNAP_DISTANCE);
			newOuter = insertIntoPolygon(newOuter, capPart.p2, SNAP_DISTANCE);
		}

		//TODO: add intersections of additional edges with outline?
		return new PolygonWithHolesXZ(newOuter, originalPolygon.getHoles());

	}

	@Override
	public Collection<VectorXZ> getInnerPoints() {
		return emptyList();
	}
	@Override
	public Collection<LineSegmentXZ> getInnerSegments() {

		List<LineSegmentXZ> innerSegments = new ArrayList<>(rings * 2 + 1);
		innerSegments.add(ridge);
		for (int i = 0; i < rings * 2; i += 2) {
			LineSegmentXZ cap1part = capParts.get(i);
			LineSegmentXZ cap2part = capParts.get(i+1);
			innerSegments.add(new LineSegmentXZ(cap1part.p1, cap2part.p2));
			innerSegments.add(new LineSegmentXZ(cap1part.p2, cap2part.p1));
		}

		return innerSegments;
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {

		double radius;
		if (roofHeight < maxDistanceToRidge) {
			double squaredHeight = roofHeight * roofHeight;
			double squaredDist = maxDistanceToRidge * maxDistanceToRidge;
			double centerY =  (squaredDist - squaredHeight) / (2 * roofHeight);
			radius = sqrt(squaredDist + centerY * centerY);
		} else {
			radius = 0;
		}

		double distRidge = distanceFromLineSegment(pos, ridge);
		double result;

		if (radius > 0) {
			double relativePlacement = distRidge / radius;
			result = roofHeight - radius + sqrt(1.0 - relativePlacement * relativePlacement) * radius;
		} else {
			// This could be any interpolator
			double relativePlacement = distRidge / maxDistanceToRidge;
			result = (1 - pow(relativePlacement, 2.5)) * roofHeight;
		}

		return max(result, 0.0);
	}
}