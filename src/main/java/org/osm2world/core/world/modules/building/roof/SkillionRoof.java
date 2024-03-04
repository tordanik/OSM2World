package org.osm2world.core.world.modules.building.roof;

import static java.lang.Math.PI;
import static java.util.Collections.*;
import static java.util.Comparator.comparingDouble;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXZ.angleBetween;

import java.util.Collection;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.Angle;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.Material;

public class SkillionRoof extends HeightfieldRoof {

	private final LineSegmentXZ ridge;
	private final double roofLength;

	public SkillionRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, double height, Material material) {

		super(originalPolygon, tags, height, material);

		SimplePolygonXZ simplifiedOuter = originalPolygon.getOuter().getSimplifiedPolygon();

		Angle angle = snapDirection(tags.getValue("roof:direction"), simplifiedOuter.getSegments());

		if (angle != null) {

			VectorXZ slopeDirection = VectorXZ.fromAngle(angle);

			/* find the "top" (upper) segment by calculating the outermost intersections of
			 * the quasi-infinite slope "line" towards the centroid vector
			 * with segments of the polygon */

			VectorXZ center = simplifiedOuter.getCentroid();

			Collection<LineSegmentXZ> intersectedSegments = simplifiedOuter.intersectionSegments(
					new LineSegmentXZ(center.add(slopeDirection.mult(-1000)), center));

			LineSegmentXZ upperSegment = max(intersectedSegments,
					comparingDouble(i -> distanceFromLineSegment(center, i)));

			/* use either the upper segment as the ridge or pick one of its end points as the top */

			if (angleBetween(upperSegment.getDirection(), slopeDirection) < PI / 180) {
				ridge = upperSegment;
			} else {
				VectorXZ offset = slopeDirection.rightNormal().mult(simplifiedOuter.getDiameter());
				LineSegmentXZ centerLine = new LineSegmentXZ(center.subtract(offset), center.add(offset));
				VectorXZ topPoint = max(upperSegment.vertices(), comparingDouble(p -> distanceFromLine(p, centerLine.p1, centerLine.p2)));
				ridge = new LineSegmentXZ(topPoint.subtract(offset), topPoint.add(offset));
			}

			/* calculate maximum distance from ridge */

			roofLength = originalPolygon.getOuter().vertices().stream()
					.mapToDouble(v -> distanceFromLine(v, ridge.p1, ridge.p2))
					.max().getAsDouble();

		} else {

			ridge = null;
			roofLength = Double.NaN;

		}

	}

	@Override
	public PolygonWithHolesXZ getPolygon() {
		return originalPolygon;
	}

	@Override
	public Collection<LineSegmentXZ> getInnerSegments() {
		return emptyList();
	}

	@Override
	public Collection<VectorXZ> getInnerPoints() {
		return emptyList();
	}

	@Override
	protected Double getRoofHeightAt_noInterpolation(VectorXZ pos) {
		if (ridge == null) {
			return roofHeight;
		} else {
			double distance = distanceFromLine(pos, ridge.p1, ridge.p2);
			double relativeDistance = distance / roofLength;
			return roofHeight - relativeDistance * roofHeight;
		}
	}



}