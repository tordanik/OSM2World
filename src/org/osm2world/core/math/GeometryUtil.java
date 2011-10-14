package org.osm2world.core.math;

import static org.osm2world.core.math.VectorXZ.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;

/**
 * utility class for some useful calculations
 */
public final class GeometryUtil {
	
	/** prevents instantiation */
	private GeometryUtil() { }
	
	public static final List<TriangleXYZ> trianglesFromVertexList(
			List<? extends VectorXYZ> vs) {
		
		if (vs.size() % 3 != 0) {
			throw new IllegalArgumentException("vertex size must be multiple of 3");
		}
		
		List<TriangleXYZ> triangles = new ArrayList<TriangleXYZ>(vs.size() / 3);
		
		for (int triangle = 0; triangle  < vs.size() / 3; triangle++) {
			
			triangles.add(new TriangleXYZ(
					vs.get(triangle * 3),
					vs.get(triangle * 3 + 1),
					vs.get(triangle * 3 + 2)));

		}

		return triangles;
		
	}
	
	public static final List<TriangleXYZ> trianglesFromTriangleStrip(
			List<? extends VectorXYZ> vs) {
		
		List<TriangleXYZ> triangles = new ArrayList<TriangleXYZ>(vs.size() - 2);
		
		for (int triangle = 0; triangle + 2 < vs.size(); triangle++) {
			
			if (triangle % 2 == 0) {
				triangles.add(new TriangleXYZ(
						vs.get(triangle),
						vs.get(triangle + 1),
						vs.get(triangle + 2)));
			} else {
				triangles.add(new TriangleXYZ(
						vs.get(triangle),
						vs.get(triangle + 2),
						vs.get(triangle + 1)));
			}

		}

		return triangles;
		
	}

	public static final List<TriangleXYZ> trianglesFromTriangleFan(
			List<? extends VectorXYZ> vs) {
		
		List<TriangleXYZ> triangles = new ArrayList<TriangleXYZ>(vs.size() - 2);

		VectorXYZ center = vs.get(0);
		
		for (int triangle = 0; triangle + 2 < vs.size(); triangle++) {
			
			triangles.add(new TriangleXYZ(
					center,
					vs.get(triangle + 1),
					vs.get(triangle + 2)));

		}

		return triangles;
		
	}
	
	/**
	 * returns the position vector where two lines intersect.
	 * The lines are given by a point on the line and a direction vector each.
	 * Result is null if the lines are parallel or have more than one common point.
	 */
	public static final VectorXZ getLineIntersection(
			VectorXZ pointA, VectorXZ directionA,
			VectorXZ pointB, VectorXZ directionB) {
		
		//TODO (documentation) explain properly
		
		//calculate dot product of directionA and directionB;
		//if dot product is (approximately) 0, the lines are parallel
		double denom = directionA.z*directionB.x - directionA.x*directionB.z;
		if(approxZero(denom)) { return null; }
		
		//TODO: why?
		denom = 1/denom;

		//calculate vector for connection between pointA and pointB
		double amcX = pointB.x-pointA.x;
		double amcZ = pointB.z-pointA.z;

		//calculate t so that intersection is at pointA+t*directionA
		//TODO: why this formula?
		double t = (amcZ*directionB.x - amcX*directionB.z)*denom;

		return new VectorXZ(
				pointA.x + t * directionA.x,
				pointA.z + t * directionA.z);
				
	}

	/**
	 * returns the position vector where two line segments intersect.
	 * The lines are given by a point on the line and a direction vector each.
	 * Result is null if the lines are parallel or have more than one common point.
	 */
	public static final VectorXZ getLineSegmentIntersection(
			VectorXZ pointA1, VectorXZ pointA2,
			VectorXZ pointB1, VectorXZ pointB2) {

		//TODO: (performance): passing "vector TO second point", rather than point2, would avoid having to calc it here - and that information could be reused for all comparisons involving the segment
		
		//TODO (documentation) explain properly
		
		double vx = pointA2.x - pointA1.x;
		double vz = pointA2.z - pointA1.z;
		double qx = pointB2.x - pointB1.x;
		double qz = pointB2.z - pointB1.z;
		
		//calculate dot product;
		//if dot product is (approximately) 0, the lines are parallel
		double denom = vz*qx - vx*qz;
		if(approxZero(denom)) { return null; }
		
		//TODO: why?
		denom = 1/denom;

		//calculate vector for connection between pointA1 and pointB1
		double amcx = pointB1.x-pointA1.x;
		double amcz = pointB1.z-pointA1.z;

		//calculate t so that intersection is at pointA1+t*v
		//TODO: why this formula?
		double t = (amcz*qx - amcx*qz)*denom;
		if( t < 0 || t > 1 ) { return null; }

		//calculate s so that intersection is at pointB1+t*q
		double s = (amcz*vx - amcx*vz)*denom;
		if( s < 0 || s > 1 ) { return null; }
		
		return new VectorXZ(
				pointA1.x + t * vx,
				pointA1.z + t * vz);
		
	}
	
	/**
	 * variant of {@link #getLineSegmentIntersection(VectorXZ, VectorXZ, VectorXZ, VectorXZ)}
	 * that also returns null (= does not announce an intersection)
	 * if the two segments share an end point
	 */
	public static final VectorXZ getTrueLineSegmentIntersection(
			VectorXZ pointA1, VectorXZ pointA2,
			VectorXZ pointB1, VectorXZ pointB2) {
	
		if (pointA1.equals(pointB1) || pointA1.equals(pointB2)
				|| pointA2.equals(pointB1) || pointA2.equals(pointB2)) {
			return null;
		} else {
			return getLineSegmentIntersection(pointA1, pointA2, pointB1, pointB2);
		}
		
	}
	
	/**
	 * returns true if the point p is on the right of the line though l1 and l2
	 */
	public static final boolean isRightOf(VectorXZ p, VectorXZ l1, VectorXZ l2) {
		
		return 0 > (p.z-l1.z) * (l2.x-l1.x) - (p.x-l1.x) * (l2.z-l1.z);
		
	}
	
	
	/**
	 * returns true if p is "between" l1 and l2,
	 * i.e. l1 to l2 is the longest side of the triangle p,l1,l2.
	 * 
	 * If all three points are on a line, this means that
	 * p is on the line segment between l1 and l2.
	 */
	public static final boolean isBetween(VectorXZ p, VectorXZ l1, VectorXZ l2) {
		
		double distSqQ1Q2 = distanceSquared(l1, l2);
		double distSqPQ1 = distanceSquared(p, l1);
		double distSqPQ2 = distanceSquared(p, l2);
		
		return distSqQ1Q2 < distSqPQ1
			&& distSqQ1Q2 < distSqPQ2;
		
	}
	
	/**
	 * returns the closest distance between point p and line segment s
	 */
	public static final double distanceFromLineSegment(VectorXZ p, LineSegmentXZ s) {
		LineSegment sJTS = new LineSegment(s.p1.x, s.p1.z, s.p2.x, s.p2.z);
		return sJTS.distance(new Coordinate(p.x, p.z));
	}

	/**
	 * returns a sequence of vectors at a distance above an original sequence
	 * 
	 * @param sequence   original sequence
	 * @param yDistance  distance in y direction between new and original sequence;
	 *                   can be negative for creating a sequence below the original sequence.
	 */
	public static final List<VectorXYZ> sequenceAbove(
			List<VectorXYZ> sequence, double yDistance) {
		
		List<VectorXYZ> newSequence = new ArrayList<VectorXYZ>(sequence.size());
		
		VectorXYZ undersideOffset = VectorXYZ.Y_UNIT.mult(yDistance);
		for (VectorXYZ v : sequence) {
			newSequence.add(v.add(undersideOffset));
		}
		
		return newSequence;
		
	}
	
	/**
	 * returns a point on a line segment between pos1 and pos2,
	 * with parameterized placement between the two end nodes.
	 * For example, a parameter of 0.5 would return the center
	 * of the line segment; the interpolated point for a parameter
	 * of 0.25 would be 1/4 of the distance between pos1 and pos2
	 * away from pos1.
	 */
	public static VectorXZ interpolateBetween(VectorXZ pos1, VectorXZ pos2, double influenceRatioPos2) {
		return new VectorXZ(
				pos1.x * (1-influenceRatioPos2) + pos2.x * influenceRatioPos2,
				pos1.z * (1-influenceRatioPos2) + pos2.z * influenceRatioPos2);
	}

	/**
	 * three-dimensional version of
	 * {@link #interpolateBetween(VectorXZ, VectorXZ, double)}
	 */
	public static VectorXYZ interpolateBetween(VectorXYZ pos1, VectorXYZ pos2, double influenceRatioPos2) {
		return new VectorXYZ(
				pos1.x * (1-influenceRatioPos2) + pos2.x * influenceRatioPos2,
				pos1.y * (1-influenceRatioPos2) + pos2.y * influenceRatioPos2,
				pos1.z * (1-influenceRatioPos2) + pos2.z * influenceRatioPos2);
	}
	
	/**
	 * performs linear interpolation of elevation information
	 * for a position on a line segment
	 */
	public static VectorXYZ interpolateElevation(VectorXZ posForEle,
			VectorXYZ pos1, VectorXYZ pos2) {
		
		double interpolatedElevation =
			interpolateValue(posForEle, pos1.xz(), pos1.y, pos2.xz(), pos2.y);
		
		return posForEle.xyz(interpolatedElevation);
		
	}
	
	/**
	 * performs linear interpolation of any value for a position on a line segment
	 */
	public static double interpolateValue(VectorXZ posForValue,
			VectorXZ pos1, double valueAt1, VectorXZ pos2, double valueAt2) {
		
		double distRatio =
			distance(pos1, posForValue)
			/ (distance(pos1, posForValue) + distance(posForValue, pos2));

		return valueAt1 * (1 - distRatio)
			+ valueAt2 * distRatio;
			
	}
	
	/**
	 * distributes points along a line segment.
	 * 
	 * This can be used for many different features, such as
	 * steps along a way, street lights along a road or posts along a fence.
	 * 
	 * @param preferredDistance  ideal distance between resulting points;
	 * this method will try to keep the actual distance as close to this as possible
	 * 
	 * @param pointsAtStartAndEnd  if true, there will be a point at lineStart
	 * and lineEnd each; if false, the closest points will be half the usual
	 * distance away from these
	 */
	public static List<VectorXZ> equallyDistributePointsAlong(
			double preferredDistance, boolean pointsAtStartAndEnd,
			VectorXZ lineStart, VectorXZ lineEnd) {
		
		double lineLength = lineStart.subtract(lineEnd).length();
		
		int numSegments = (int) Math.round(lineLength / preferredDistance);

		if (numSegments == 0) {
			return new ArrayList<VectorXZ>(0);
		}
						
		double pointDistance = lineLength / numSegments;
		VectorXZ pointDiff = lineEnd.subtract(lineStart).normalize().mult(pointDistance);
				
		int numPoints = pointsAtStartAndEnd ? numSegments + 1 : numSegments;
		List<VectorXZ> result = new ArrayList<VectorXZ>(numPoints);
		
		/* create the points, starting with the first and basing each on the previous */
		
		VectorXZ mostRecentPoint = pointsAtStartAndEnd ?
				lineStart :
				lineStart.add(pointDiff.mult(0.5f));
		
		result.add(mostRecentPoint);

		for (int point = 1; point < numPoints; point ++) {
			
			mostRecentPoint = mostRecentPoint.add(pointDiff);
			result.add(mostRecentPoint);
						
		}
		
		return result;
		
	}

	/**
	 * distributes points along a line segment sequence.
	 * 
	 * This can be used for many different features, such as
	 * steps along a way, street lights along a road or posts along a fence.
	 * 
	 * @param preferredDistance  ideal distance between resulting points;
	 * this method will try to keep the actual distance as close to this as possible
	 * 
	 * @param pointsAtStartAndEnd  if true, there will be a point at lineStart
	 * and lineEnd each; if false, the closest points will be half the usual
	 * distance away from these
	 */
	public static List<VectorXZ> equallyDistributePointsAlong(
			double preferredDistance, boolean pointsAtStartAndEnd,
			List<VectorXZ> points) {
		
		double length = 0;
		for (int i=0; i+1 < points.size(); i++) {
			length += points.get(i+1).subtract(points.get(i)).length();
		}
		
		int numSegments = (int) Math.round(length / preferredDistance);
		
		if (numSegments == 0) {
			return Collections.<VectorXZ>emptyList();
		}
		
		double pointDistance = length / numSegments;
				
		int numPoints = pointsAtStartAndEnd ? numSegments + 1 : numSegments;
		List<VectorXZ> result = new ArrayList<VectorXZ>(numPoints);
		
		/* create the points */
		
		double currentDistanceFromStart = pointsAtStartAndEnd ? 0 : pointDistance / 2;

		for (int point = 1; point < numPoints; point ++) {
			
			//TODO: create and add point (to result) based on currentDistanceFromStart
			
			currentDistanceFromStart += pointDistance;
			
		}
		
		return result;
		
		//TODO: support distributing along a line with corners etc,
		//to avoid "restart" at each node
		//(should be relatively easy, just calculate TOTAL line length first
		// and then proceed in a way similar to getEleAt)
				
	}
	
	/**
	 * returns a polygon that is constructed from a given polygon
	 * by inserting a point into one of the segments of the original polygon.
	 * The segment chosen for this purpose is the one closest to the new node.
	 * 
	 * @param polygon       original polygon, will not be modified by this method
	 * @param point         the new point
	 * @param snapDistance  minimum distance of new point from segment endpoints;
	 *                      if the new point is closer, the unmodified
	 *                      original polygon will be returned.
	 */
	public static PolygonXZ insertIntoPolygon(PolygonXZ polygon,
			VectorXZ point, double snapDistance) {
		
		LineSegmentXZ segment = polygon.getClosestSegment(point);
		
		for (int i = 0; i + 1 <= polygon.size() ; i++) {
			
			if (polygon.getVertex(i).equals(segment.p1)
					&& polygon.getVertexAfter(i).equals(segment.p2)){
				
				if (polygon.getVertex(i).distanceTo(point) <= snapDistance
						|| polygon.getVertexAfter(i).distanceTo(point) <= snapDistance) {
					
					return polygon;
					
				} else {

					ArrayList<VectorXZ> vertexLoop =
						new ArrayList<VectorXZ>(polygon.getVertexLoop());
					
					vertexLoop.add(i + 1, point);
					
					return new PolygonXZ(vertexLoop);
					
				}
				
			}
			
		}
		
		throw new IllegalArgumentException("segment " + segment +
				" was not found in polygon " + polygon);
		
	}
	
	/**
	 * maximum number of consecutive failures to place a point during random
	 * distribution to be tolerated before stopping the process.
	 */
	private static final int MAX_FAILED_ATTEMPTS = 500;

	/**
	 * distributes points randomly on a polygon area.
	 * 
	 * This can be used for different features, such as
	 * trees in a forest.
	 * 
	 * Note that the density isn't guaranteed. After several failed attempts
	 * to place a point, the distribution will stop even if the density
	 * hasn't been reached.
	 * 
	 * @param polygonWithHolesXZ  polygon on which the points should be placed
	 * @param density             desired number of points per unit of area
	 * @param minimumDistance     minimum distance between resulting points
	 */
	public static List<VectorXZ> randomlyDistributePointsOn(
			PolygonWithHolesXZ polygonWithHolesXZ, double density, double minimumDistance) {
		
		int pointsToDistribute = (int)(polygonWithHolesXZ.getArea() * density);
		
		/* calculate axis-aligned boundaries */
		
		List<VectorXZ> vertices = polygonWithHolesXZ.getOuter().getVertices();
		
		AxisAlignedBoundingBoxXZ box = new AxisAlignedBoundingBoxXZ(vertices);
		
		/* randomly choose points in the boundaries
		 * and check whether they are contained in the polygon */
		
		List<VectorXZ> result = new ArrayList<VectorXZ>(pointsToDistribute);

		int failedAttempts = 0;
		
		while (result.size() < pointsToDistribute
				&& failedAttempts <= MAX_FAILED_ATTEMPTS) {
			
			double x = box.minX + (box.maxX - box.minX) * Math.random();
			double z = box.minZ + (box.maxZ - box.minZ) * Math.random();
			
			VectorXZ v = new VectorXZ(x, z);
			
			if (polygonWithHolesXZ.contains(v)) {
				
				//TODO: check minimumDistance
				
				result.add(v);
				failedAttempts = 0;
				
			} else {
				failedAttempts ++;
			}
			
		}
		
		return result;
		
	}
		
	private static final double EPSILON = 0.0001f;
	
	private static final boolean approxZero(double f) {
		return f <= EPSILON && f >= -EPSILON;
	}
	
}
