package org.osm2world.core.world.modules.building.roof;

import static java.lang.Math.*;
import static java.util.Collections.max;
import static java.util.Comparator.comparingDouble;
import static org.osm2world.core.math.GeometryUtil.distanceFromLineSegment;
import static org.osm2world.core.util.ValueParseUtil.parseAngle;

import java.util.Collection;
import java.util.Iterator;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.Material;

/**
 * tagged roof with a ridge.
 * Deals with ridge calculation for various subclasses.
 */
abstract public class RoofWithRidge extends HeightfieldRoof {

	/** absolute distance of ridge to outline */
	protected final double ridgeOffset;

	protected final LineSegmentXZ ridge;

	/** the roof cap that is closer to the first vertex of the ridge */
	protected final LineSegmentXZ cap1;
	/** the roof cap that is closer to the second vertex of the ridge */
	protected final LineSegmentXZ cap2;

	/** maximum distance of any outline vertex to the ridge */
	protected final double maxDistanceToRidge;

	/**
	 * creates an instance and calculates the final fields
	 *
	 * @param relativeRoofOffset  distance of ridge to outline
	 *    relative to length of roof cap; 0 if ridge ends at outline
	 * @param tags
	 */
	public RoofWithRidge(double relativeRoofOffset, PolygonWithHolesXZ originalPolygon,
			TagSet tags, double height, Material material) {

		super(originalPolygon, tags, height, material);

		SimplePolygonXZ outerPoly = originalPolygon.getOuter();

		SimplePolygonXZ simplifiedPolygon = outerPoly.getSimplifiedPolygon();

		/* determine ridge direction based on tag if it exists,
		 * otherwise choose direction of longest polygon segment */

		VectorXZ ridgeDirection = null;

		if (tags.containsKey("roof:direction")) {
			Double angle = parseAngle(tags.getValue("roof:direction"));
			if (angle != null) {
				ridgeDirection = VectorXZ.fromAngle(toRadians(angle)).rightNormal();
			}
		}

		if (ridgeDirection == null && tags.containsKey("roof:ridge:direction")) {
			Double angle = parseAngle(tags.getValue("roof:ridge:direction"));
			if (angle != null) {
				ridgeDirection = VectorXZ.fromAngle(toRadians(angle));
			}
		}

		if (ridgeDirection == null) {

			LineSegmentXZ longestSeg = max(simplifiedPolygon.getSegments(), comparingDouble(s -> s.getLength()));

			ridgeDirection = longestSeg.p2.subtract(longestSeg.p1).normalize();

			if (tags.contains("roof:orientation", "across")) {
				ridgeDirection = ridgeDirection.rightNormal();
			}

		}

		/* calculate the two outermost intersections of the
		 * quasi-infinite ridge line with segments of the polygon */

		VectorXZ p1 = outerPoly.getCentroid();

		Collection<LineSegmentXZ> intersections =
			simplifiedPolygon.intersectionSegments(new LineSegmentXZ(
					p1.add(ridgeDirection.mult(-1000)),
					p1.add(ridgeDirection.mult(1000))
			));

		if (intersections.size() < 2) {
			throw new InvalidGeometryException("cannot handle roof geometry");
		}

		//TODO choose outermost instead of any pair of intersections
		Iterator<LineSegmentXZ> it = intersections.iterator();
		cap1 = it.next();
		cap2 = it.next();

		/* base ridge on the centers of the intersected segments
		 * (the intersections itself are not used because the
		 * tagged ridge direction is likely not precise)       */

		VectorXZ c1 = cap1.getCenter();
		VectorXZ c2 = cap2.getCenter();

		ridgeOffset = min(
				cap1.getLength() * relativeRoofOffset,
				0.4 * c1.distanceTo(c2));

		if (relativeRoofOffset == 0) {

			ridge = new LineSegmentXZ(c1, c2);

		} else {

			ridge = new LineSegmentXZ(
					c1.add( p1.subtract(c1).normalize().mult(ridgeOffset) ),
					c2.add( p1.subtract(c2).normalize().mult(ridgeOffset) ));

		}

		/* calculate maxDistanceToRidge */

		maxDistanceToRidge = outerPoly.getVertices().stream()
				.mapToDouble(v -> distanceFromLineSegment(v, ridge))
				.max().getAsDouble();

	}

}