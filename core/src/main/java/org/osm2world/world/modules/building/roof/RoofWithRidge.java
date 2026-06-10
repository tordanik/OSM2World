package org.osm2world.world.modules.building.roof;

import static java.lang.Math.min;
import static java.lang.Math.tan;
import static java.util.Collections.max;
import static java.util.Comparator.comparingDouble;
import static org.osm2world.math.algorithms.GeometryUtil.distanceFromLineSegment;
import static org.osm2world.util.ValueParseUtil.parseMeasure;

import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.Angle;
import org.osm2world.math.Intersection;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.*;
import org.osm2world.scene.material.Material;
import org.osm2world.util.exception.InvalidGeometryException;
import org.osm2world.world.modules.building.BuildingPart;

/**
 * roof with a ridge which has been described with a roof:shape tag rather than explicitly mapped ridge geometry.
 * Deals with ridge calculation for various subclasses.
 */
abstract public class RoofWithRidge extends RoofWithInnerLines {

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
	 */
	public RoofWithRidge(@Nullable BuildingPart buildingPart, double relativeRoofOffset,
			PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {

		super(buildingPart, originalPolygon, tags, material);

		SimplePolygonXZ outerPoly = originalPolygon.getOuter();

		SimplePolygonXZ simplifiedPolygon = outerPoly.getSimplifiedPolygon();

		VectorXZ ridgeDirection = ridgeDirectionFromTags(tags, simplifiedPolygon);

		/* calculate the two outermost intersections of the
		 * quasi-infinite ridge line with segments of the polygon */

		var bbox = AxisAlignedRectangleXZ.bbox(outerPoly.vertices());

		VectorXZ p1 = outerPoly.getCentroid();
		LineSegmentXZ intersectionLine = new LineSegmentXZ(
				p1.add(ridgeDirection.mult(-(bbox.sizeX() + bbox.sizeZ()))),
				p1.add(ridgeDirection.mult(bbox.sizeX() + bbox.sizeZ()))
		);

		List<Intersection> intersections = simplifiedPolygon.intersections(intersectionLine);

		if (intersections.size() < 2) {
			throw new InvalidGeometryException("cannot handle roof geometry");
		}

		intersections.sort(comparingDouble(i -> i.point().distanceTo(intersectionLine.p1)));
		Intersection i1 = intersections.get(0);
		Intersection i2 = intersections.get(intersections.size() - 1);

		cap1 = i1.segment();
		cap2 = i2.segment();

		VectorXZ c1 = i1.point();
		VectorXZ c2 = i2.point();

		/* consider an offset for ridges which do not end at the wall of the building */

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


	/** Determine ridge direction based on one of several supported tags */
	static VectorXZ ridgeDirectionFromTags(TagSet tags, SimplePolygonXZ simplifiedPolygon) {

		VectorXZ ridgeDirection = ridgeVectorFromRoofDirection(tags, simplifiedPolygon);

		if (ridgeDirection == null) {
			ridgeDirection = ridgeVectorFromRidgeDirection(tags, simplifiedPolygon);
		}

		if (ridgeDirection == null) {
			ridgeDirection = ridgeVectorFromRoofOrientation(tags, simplifiedPolygon);
		}

		return ridgeDirection;

	}

	/**
	 * returns a ridge direction vector based on the roof:direction tag, if possible.
	 * May modify the direction a bit to "snap" to directions parallel or orthogonal to polygon segments.
	 */
	private static @Nullable VectorXZ ridgeVectorFromRoofDirection(TagSet tags, SimplePolygonXZ polygon) {
		Angle angle = snapDirection(tags.getValue("roof:direction"), polygon.getSegments());
		if (angle == null) {
			return null;
		} else {
			return VectorXZ.fromAngle(angle).rightNormal();
		}
	}

	/**
	 * returns a ridge direction vector based on the roof:ridge:direction tag, if possible.
	 */
	private static VectorXZ ridgeVectorFromRidgeDirection(TagSet tags, SimplePolygonXZ polygon) {
		Angle angle = snapDirection(tags.getValue("roof:ridge:direction"), polygon.getSegments());
		return angle != null ? VectorXZ.fromAngle(angle) : null;
	}

	/**
	 * returns a ridge direction vector based on the roof:orientation tag.
	 * If that tag is not set or has an unknown value, roof:orientation=along is assumed.
	 */
	private static VectorXZ ridgeVectorFromRoofOrientation(TagSet tags,
			SimplePolygonXZ polygon) {

		RectangleXZ rotatedBbox = polygon.minimumRotatedBoundingBox();
		LineSegmentXZ longestSeg = max(rotatedBbox.getSegments(), comparingDouble(s -> s.getLength()));

		VectorXZ result = longestSeg.p2.subtract(longestSeg.p1).normalize();

		if (tags.contains("roof:orientation", "across")) {
			result = result.rightNormal();
		}

		return result;

	}

	@Override
	public Double calculatePreliminaryHeight() {

		Double roofHeight = parseMeasure(tags.getValue("roof:height"));

		if (roofHeight == null) {
			Angle angle = parseRoofAngle(tags);
			if (angle != null) {
				roofHeight = tan(angle.radians) * maxDistanceToRidge;
			}
		}

		return roofHeight;

	}

}