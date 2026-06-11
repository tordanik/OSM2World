package org.osm2world.world.modules.building.roof;

import static java.lang.Math.*;
import static java.util.Collections.max;
import static java.util.Comparator.comparingDouble;
import static java.util.Objects.requireNonNullElse;
import static org.osm2world.math.algorithms.GeometryUtil.distanceFromLineSegment;
import static org.osm2world.math.algorithms.GeometryUtil.isRightOf;
import static org.osm2world.util.ValueParseUtil.parseMeasure;

import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.Angle;
import org.osm2world.math.Intersection;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.RectangleXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.util.exception.InvalidGeometryException;
import org.osm2world.world.modules.building.BuildingPart;

/**
 * roof with a ridge which has been described with a roof:shape tag rather than explicitly mapped ridge geometry.
 * Deals with ridge calculation for various subclasses.
 */
abstract public class RoofWithRidge extends RoofWithInnerLines {

	/** absolute distance of ridge to outline at the start */
	protected final double ridgeOffset1;

	/** absolute distance of ridge to outline at the end */
	protected final double ridgeOffset2;

	protected final LineSegmentXZ ridge;

	/** the roof cap that is closer to the first vertex of the ridge. Points to the right of the ridge. */
	protected final LineSegmentXZ cap1;
	/** the roof cap that is closer to the second vertex of the ridge. Points to the right of the ridge. */
	protected final LineSegmentXZ cap2;

	/** maximum distance of any outline vertex to the ridge */
	protected final double maxDistanceToRidge;


	/**
	 * creates an instance and calculates the final fields
	 *
	 * @param relativeRidgeOffset  distance of ridge to outline relative to length of roof cap;
	 *                             0 if ridge touches outline
	 */
	public RoofWithRidge(@Nullable BuildingPart buildingPart, double relativeRidgeOffset,
			PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		this(buildingPart, relativeRidgeOffset, relativeRidgeOffset, false, null,
				originalPolygon, tags, material);
	}

	/**
	 * Variant of the constructor for certain more unusual roof shapes.
	 * Supports separate offsets for the start and end of the ridge, the ability to move the ridge forward/backward,
	 * and different roof:direction interpretations.
	 *
	 * @param ridgeAlongDirection   whether the ridge goes along the roof:direction instead of being orthogonal to it
	 * @param relativeRidgeLocation where between the front and back of the roof the ridge is placed
	 *                              (0.5: in the center, 1.0: in the back).
	 */
	public RoofWithRidge(@Nullable BuildingPart buildingPart, double relativeRidgeOffset1,
			double relativeRidgeOffset2, boolean ridgeAlongDirection, @Nullable Double relativeRidgeLocation,
			PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {

		super(buildingPart, originalPolygon, tags, material);

		relativeRidgeLocation = requireNonNullElse(relativeRidgeLocation, 0.5);

		SimplePolygonXZ outerPoly = originalPolygon.getOuter();

		SimplePolygonXZ simplifiedPolygon = outerPoly.getSimplifiedPolygon();

		VectorXZ ridgeDirection = ridgeDirectionFromTags(tags, ridgeAlongDirection,
				simplifiedPolygon, defaultDirection());

		/* calculate the two outermost intersections of the
		 * quasi-infinite ridge line with segments of the polygon */

		var bbox = outerPoly.rotatedBoundingBox(Angle.ofRadians(ridgeDirection.angle()));

		VectorXZ p1 = outerPoly.getCentroid();

		LineSegmentXZ toBack = bbox.getSegments().get(3);
		p1 = p1.add((toBack.p2.subtract(toBack.p1)).mult(relativeRidgeLocation - 0.5));

		LineSegmentXZ intersectionLine = new LineSegmentXZ(
				p1.add(ridgeDirection.mult(-bbox.getDiameter())),
				p1.add(ridgeDirection.mult(bbox.getDiameter()))
		);

		List<Intersection> intersections = simplifiedPolygon.intersections(intersectionLine);

		if (intersections.size() < 2) {
			throw new InvalidGeometryException("cannot handle roof geometry");
		}

		intersections.sort(comparingDouble(i -> i.point().distanceTo(intersectionLine.p1)));
		Intersection i1 = intersections.get(0);
		Intersection i2 = intersections.get(intersections.size() - 1);

		VectorXZ c1 = i1.point();
		VectorXZ c2 = i2.point();

		/* consider an offset for ridges which do not end at the wall of the building */

		ridgeOffset1 = min(
				i1.segment().getLength() * relativeRidgeOffset1,
				0.4 * c1.distanceTo(c2));

		ridgeOffset2 = min(
				i2.segment().getLength() * relativeRidgeOffset2,
				0.4 * c1.distanceTo(c2));

		if (ridgeOffset1 == 0 && ridgeOffset2 == 0) {

			ridge = new LineSegmentXZ(c1, c2);

		} else {

			ridge = new LineSegmentXZ(
					c1.add( p1.subtract(c1).normalize().mult(ridgeOffset1) ),
					c2.add( p1.subtract(c2).normalize().mult(ridgeOffset2) ));

		}

		cap1 = isRightOf(i1.segment().p2, ridge.p1, ridge.p2) ? i1.segment() : i1.segment().reverse();
		cap2 = isRightOf(i2.segment().p2, ridge.p1, ridge.p2) ? i2.segment() : i2.segment().reverse();

		/* calculate maxDistanceToRidge */

		maxDistanceToRidge = outerPoly.getVertices().stream()
				.mapToDouble(v -> distanceFromLineSegment(v, ridge))
				.max().getAsDouble();

	}

	/** Determine ridge direction based on one of several supported tags */
	static VectorXZ ridgeDirectionFromTags(TagSet tags, boolean ridgeAlongDirection,
			SimplePolygonXZ simplifiedPolygon, @Nullable VectorXZ defaultDirection) {

		VectorXZ ridgeDirection = ridgeVectorFromRoofDirection(tags, ridgeAlongDirection, simplifiedPolygon);

		if (ridgeDirection == null) {
			ridgeDirection = ridgeVectorFromRidgeDirection(tags, simplifiedPolygon);
		}

		if (ridgeDirection == null) {
			if (defaultDirection == null) {
				ridgeDirection = ridgeVectorFromRoofOrientation(tags, simplifiedPolygon);
			} else if (tags.containsKey("roof:orientation")) {
				ridgeDirection = ridgeVectorFromRoofOrientation(tags, simplifiedPolygon);
				// use defaultDirection to pick between the two possible directions for a roof:orientation value
				if (VectorXZ.angleBetween(ridgeDirection, defaultDirection) > 0.9 * PI) {
					ridgeDirection = ridgeDirection.invert();
				}
			} else {
				ridgeDirection = defaultDirection;
			}
		}

		return ridgeDirection;

	}

	/**
	 * returns a ridge direction vector based on the roof:direction tag, if possible.
	 * May modify the direction a bit to "snap" to directions parallel or orthogonal to polygon segments.
	 */
	private static @Nullable VectorXZ ridgeVectorFromRoofDirection(TagSet tags, boolean ridgeAlongDirection,
			SimplePolygonXZ polygon) {
		Angle angle = snapDirection(tags.getValue("roof:direction"), polygon.getSegments());
		if (angle == null) {
			return null;
		} else {
			if (ridgeAlongDirection) {
				return VectorXZ.fromAngle(angle);
			} else {
				return VectorXZ.fromAngle(angle).rightNormal();
			}
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

	/**
	 * returns the default direction if no explicit roof:direction tag is set.
	 * Only intended for a subset of roof shapes which have special logic for this.
	 */
	protected @Nullable VectorXZ defaultDirection() {
		return null;
	}

}