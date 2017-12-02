package org.osm2world.core.math;

import static java.lang.Math.sqrt;
import static java.util.Collections.emptyList;
import static org.osm2world.core.math.VectorXZ.*;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
		
		return trianglesFromVertexList(
				triangleVertexListFromTriangleStrip(vs));
		
	}
	
	public static final <V> List<V> triangleVertexListFromTriangleStrip(
			List<? extends V> vs) {
		
		List<V> result = new ArrayList<V>((vs.size() - 2) * 3);
		
		for (int triangle = 0; triangle + 2 < vs.size(); triangle++) {
			
			if (triangle % 2 == 0) {
				result.add(vs.get(triangle));
				result.add(vs.get(triangle + 1));
				result.add(vs.get(triangle + 2));
			} else {
				result.add(vs.get(triangle));
				result.add(vs.get(triangle + 2));
				result.add(vs.get(triangle + 1));
			}

		}

		return result;
		
	}
	
	public static final <V> List<V> triangleNormalListFromTriangleStripOrFan(
			List<? extends V> normals) {
		
		List<V> result = new ArrayList<V>((normals.size() - 2) * 3);
		
		for (int triangle = 0; triangle + 2 < normals.size(); triangle++) {
			V normal = normals.get(triangle + 2);
			result.add(normal);
			result.add(normal);
			result.add(normal);
		}
		
		return result;
		
	}

	public static final List<TriangleXYZ> trianglesFromTriangleFan(
			List<? extends VectorXYZ> vs) {

		return trianglesFromVertexList(
				triangleVertexListFromTriangleFan(vs));
		
	}

	public static final <V> List<V> triangleVertexListFromTriangleFan(
			List<? extends V> vs) {
		
		List<V> result = new ArrayList<V>((vs.size() - 2) * 3);

		V center = vs.get(0);
		
		for (int triangle = 0; triangle + 2 < vs.size(); triangle++) {

			result.add(center);
			result.add(vs.get(triangle + 1));
			result.add(vs.get(triangle + 2));

		}

		return result;
		
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
		
		double distSqL1L2 = distanceSquared(l1, l2);
		double distSqPL1 = distanceSquared(p, l1);
		double distSqPL2 = distanceSquared(p, l2);
		
		return distSqL1L2 > distSqPL1
			&& distSqL1L2 > distSqPL2;
		
	}
	
	/**
	 * returns the closest distance between point p and a line defined by two points
	 */
	public static final double distanceFromLine(VectorXZ p,	VectorXZ v1, VectorXZ v2) {
		return Line2D.ptLineDist(v1.x, v1.z, v2.x, v2.z, p.x, p.z);
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
	 * distributes points along a 3D line segment sequence.
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
	public static List<VectorXYZ> equallyDistributePointsAlong(
			double preferredDistance, boolean pointsAtStartAndEnd,
			List<VectorXYZ> points) {
		
		double length = 0;
		for (int i=0; i+1 < points.size(); i++) {
			length += points.get(i).distanceToXZ(points.get(i+1));
		}
		
		int numSegments = (int) Math.round(length / preferredDistance);
		
		if (numSegments == 0) {
			return emptyList();
		}
		
		double pointDistance = length / numSegments;
				
		int numPoints = pointsAtStartAndEnd ? numSegments + 1 : numSegments;
		List<VectorXYZ> result = new ArrayList<VectorXYZ>(numPoints);
		
		/* create the points */
		
		double currentDistanceFromStart = pointsAtStartAndEnd ? 0 : pointDistance / 2;

		int currentInputPoint = 0;
		double offsetCurrentInputPoint = 0;
		double offsetNextInputPoint = points.get(0).distanceToXZ(points.get(1));
		
		for (int i = 0; i < numPoints; i ++) {
			
			while (currentDistanceFromStart > offsetNextInputPoint) {
				
				//proceed to the next segment of the input point sequence
				//(note: there will always be a next segment if this code is reached due to the condition)
				
				currentInputPoint += 1;
				offsetCurrentInputPoint = offsetNextInputPoint;
				
				offsetNextInputPoint = offsetCurrentInputPoint
						+ points.get(currentInputPoint).distanceToXZ(points.get(currentInputPoint + 1));
				
			}
			
			//create the point
			
			VectorXYZ p = interpolateBetween(points.get(currentInputPoint), points.get(currentInputPoint + 1),
					(currentDistanceFromStart - offsetCurrentInputPoint) / (offsetNextInputPoint - offsetCurrentInputPoint));
			
			result.add(p);
			
			//increase the distance from start by one step to prepare for the next point
			
			currentDistanceFromStart += pointDistance;
			
		}
		
		return result;
		
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
	 * constant used by {@link #distributePointsOn(long, PolygonWithHolesXZ,
	 *  AxisAlignedBoundingBoxXZ, double, double)}
	 */
	private static final int POINTS_PER_BOX = 100;
	
	/**
	 * distributes points pseudo-randomly on a polygon area.
	 * The distribution for a set of parameters will always be identical.
	 * 
	 * This can be used for features such as trees in a forest.
	 * 
	 * Distribution works by slicing the area's bounding box into smaller boxes
	 * with POINTS_PER_BOX potential points each, the size of these boxes
	 * depending on density. In each of the boxes, POINTS_PER_BOX pseudo-random
	 * positions will be calculated. If a position is far enough from previous
	 * ones and not inside a hole, the position will be contained in the result.
	 * 
	 * @param seed                a seed for random number generation
	 * @param polygonWithHolesXZ  polygon on which the points should be placed
	 * @param boundary            boundary of the relevant area or null;
	 *                            points outside of the boundary are optional.
	 * @param density             desired number of points per unit of area
	 * @param minimumDistance     minimum distance between resulting points
	 *                            (not yet implemented)
	 */
	public static List<VectorXZ> distributePointsOn(
			long seed, PolygonWithHolesXZ polygonWithHolesXZ,
			AxisAlignedBoundingBoxXZ boundary,
			double density,	double minimumDistance) {
		
		List<VectorXZ> result = new ArrayList<VectorXZ>();

		Random rand = new Random(seed);
		
		AxisAlignedBoundingBoxXZ outerBox = new AxisAlignedBoundingBoxXZ(
				polygonWithHolesXZ.getOuter().getVertices());
		
		double boxSize = sqrt(100 / density);
		
		for (int boxZ = 0; boxZ <= (int)(outerBox.sizeZ() / boxSize); ++boxZ) {
			for (int boxX = 0; boxX <= (int)(outerBox.sizeX() / boxSize); ++boxX) {
				
				AxisAlignedBoundingBoxXZ box = new AxisAlignedBoundingBoxXZ(
						outerBox.minX + boxSize * boxX,
						outerBox.minZ + boxSize * boxZ,
						outerBox.minX + boxSize * (boxX + 1),
						outerBox.minZ + boxSize * (boxZ + 1));
				
				if (boundary != null && !boundary.overlaps(box)) {
					continue;
				}
				
				if (!polygonWithHolesXZ.contains(box.polygonXZ())
						&& !polygonWithHolesXZ.intersects(box.polygonXZ())) {
					continue;
				}
				
				for (int i = 0; i < POINTS_PER_BOX; ++i) {
					
					double x = box.minX + boxSize * rand.nextDouble();
					double z = box.minZ + boxSize * rand.nextDouble();
					
					VectorXZ v = new VectorXZ(x, z);
					
					if (polygonWithHolesXZ.contains(v)) {
						
						//TODO: check minimumDistance
						
						result.add(v);
						
					}
					
				}

			}
		}
		
		return result;
		
	}
		
	private static final double EPSILON = 0.0001f;
	
	private static final boolean approxZero(double f) {
		return f <= EPSILON && f >= -EPSILON;
	}
	
	/**
	 * Calculate consistent tangent vectors for given vertices and vertex normals and texture coordinates.
	 * Code inspired by Lengyel, Eric. “Computing Tangent Space Basis Vectors for an Arbitrary Mesh”. Terathon Software 3D Graphics Library, 2001. http://www.terathon.com/code/tangent.html
	 * @param vertices list of vertices for which the tangent vectors get calculated
	 * @param normals list of normals belonging to the vertices
	 * @param texCoords list of texture coordinates belonging to the vertices
	 * @return list of tangent vectors that are consistent with the vertices, normals and texture coordinates
	 */
	public static final List<VectorXYZW> calculateTangentVectorsForTexLayer(List<VectorXYZ> vertices, List<VectorXYZ> normals,
			List<VectorXZ> texCoords)
	{
		int vertexCount = vertices.size();
		VectorXYZ[] tan1 = new VectorXYZ[vertexCount];
		VectorXYZ[] tan2 = new VectorXYZ[vertexCount];
		List<VectorXYZW> tangents = new ArrayList<VectorXYZW>();
	    
	    for (int a = 0; a < vertexCount / 3; a++)
	    {
	    	int i = a*3+1;
	    	VectorXYZ v1 = vertices.get(i-1);
	    	VectorXYZ v2 = vertices.get(i);
	    	VectorXYZ v3 = vertices.get(i+1);

	    	VectorXZ w1 = texCoords.get(i-1);
	    	VectorXZ w2 = texCoords.get(i);
	    	VectorXZ w3 = texCoords.get(i+1);
	        
	        double x1 = v2.x - v1.x;
	        double x2 = v3.x - v1.x;
	        double y1 = v2.y - v1.y;
	        double y2 = v3.y - v1.y;
	        double z1 = v2.z - v1.z;
	        double z2 = v3.z - v1.z;
	        
	        double s1 = w2.x - w1.x;
	        double s2 = w3.x - w1.x;
	        double t1 = w2.z - w1.z;
	        double t2 = w3.z - w1.z;
	        
	        double r = 1.0 / (s1 * t2 - s2 * t1);
	        VectorXYZ sdir = new VectorXYZ((t2 * x1 - t1 * x2) * r, (t2 * y1 - t1 * y2) * r,
	                (t2 * z1 - t1 * z2) * r);
	        VectorXYZ tdir = new VectorXYZ((s1 * x2 - s2 * x1) * r, (s1 * y2 - s2 * y1) * r,
	                (s1 * z2 - s2 * z1) * r);
	        
	        tan1[i-1] = sdir;
	        tan1[i] = sdir;
	        tan1[i+1] = sdir;
	        
	        tan2[i-1] = tdir;
	        tan2[i] = tdir;
	        tan2[i+1] = tdir;
	    }
	    
	    for (int a = 0; a < vertexCount; a++)
	    {
	        VectorXYZ n = normals.get(a);
	        VectorXYZ t = tan1[a];
	        // Gram-Schmidt orthogonalize
	        VectorXYZ res = (t.subtract(n.mult(n.dot(t)))).normalize();

	        // Calculate handedness
	        double w = (n.cross(t).dot(tan2[a]) < 0.0F) ? -1.0F : 1.0F;
	        
	        tangents.add(new VectorXYZW(res.x, res.y, res.z, w));
	    }
	    
	    return tangents;
	}
	
	/**
	 * Epsilon to move the shadow volumes away in light direction. Avoids self shadowing.
	 */
	private static final double SHADOWVOLUME_EPSILON = 0.1f;
	
	/**
	 * Calculate per triangle shadow volumes. Only light facing triangles will create a shadow volume, so objects should probably be closed for correct results.
	 * @param vertices list of the triangles vertices. Have to be a multiple of three.
	 * @param lightPos position of the light source. Supports point lights with lightPos.w != 0 and directional lights with lightPos.w == 0
	 * @return list of the shadow volume vertices in homogeneous coordinates. These will contain points at infinity (w == 0) for the shadow volume back cap
	 */
	public static final List<VectorXYZW> calculateShadowVolumesPerTriangle(List<VectorXYZ> vertices, VectorXYZW lightPos) {
		
		List<VectorXYZW> shadowVolumes = new ArrayList<VectorXYZW>();

		for (int triangle = 0; triangle < vertices.size()/3; triangle++) {
			VectorXYZ v1 = vertices.get(triangle*3);
			VectorXYZ v2 = vertices.get(triangle*3+1);
			VectorXYZ v3 = vertices.get(triangle*3+2);
			
			VectorXYZ normal = v1.subtract(v2).crossNormalized(v3.subtract(v2));
			
			VectorXYZ lightDir;
			if (lightPos.w == 0)
				lightDir = lightPos.xyz().mult(-1).normalize();
			else
				lightDir = v1.subtract(lightPos.xyz().mult(1/lightPos.w)).normalize();
			
			// skip parallel triangles
			if (approxZero(normal.dot(lightDir)))
				continue;
			
			// only light facing triangles (not the same direction of the light rays) throw shadows
			if (normal.dot(lightDir) < 0) {
				
				// revert light facing triangles
				VectorXYZ tmp = v2;
				v2 = v3;
				v3 = tmp;
			//}
			
				// calculate the volume sides
				shadowVolumes.addAll(emitQuad(v1, v2, lightPos));
				shadowVolumes.addAll(emitQuad(v2, v3, lightPos));
				shadowVolumes.addAll(emitQuad(v3, v1, lightPos));

				// calculate the front/back cap. move front cap a little in light direction to avoid self shadowing
				VectorXYZW c1 = new VectorXYZW( v1.add(lightDir.mult(SHADOWVOLUME_EPSILON)), 1);
				VectorXYZW b1 = new VectorXYZW( lightDir, 0);

				if (lightPos.w != 0)
					lightDir = v2.subtract(lightPos.xyz().mult(1/lightPos.w)).normalize();
				VectorXYZW c2 = new VectorXYZW( v2.add(lightDir.mult(SHADOWVOLUME_EPSILON)), 1);
				VectorXYZW b2 = new VectorXYZW( lightDir, 0);

				if (lightPos.w != 0)
					lightDir = v3.subtract(lightPos.xyz().mult(1/lightPos.w)).normalize();
				VectorXYZW c3 = new VectorXYZW( v3.add(lightDir.mult(SHADOWVOLUME_EPSILON)), 1);
				VectorXYZW b3 = new VectorXYZW( lightDir, 0);

				shadowVolumes.add(c1);
				shadowVolumes.add(c2);
				shadowVolumes.add(c3);

				// reverse back cap order
				shadowVolumes.add(b1);
				shadowVolumes.add(b3);
				shadowVolumes.add(b2);
			}
			
		}
		
		return shadowVolumes;
	}
	
	/**
	 * Creates the sides of a shadow volume for a edge of the triangle casting the shadow.
	 * @param start the start of the triangle edge
	 * @param end the end of the triangle edge
	 * @param lightPos position of the shadow casting light
	 * @return the vertices making up the side of the shadow volume for the triangle edge
	 */
	private static List<VectorXYZW> emitQuad(VectorXYZ start, VectorXYZ end, VectorXYZW lightPos) {

		List<VectorXYZW> result = new ArrayList<VectorXYZW>(10);
		VectorXYZ lightDir;
		
		// Vertex #1: the starting vertex (just a tiny bit below the original edge)
		if (lightPos.w == 0)
			lightDir = lightPos.xyz().mult(-1).normalize();
		else
			lightDir = start.subtract(lightPos.xyz().mult(1/lightPos.w)).normalize();
		
		VectorXYZW v1 = new VectorXYZW(start.add(lightDir.mult(SHADOWVOLUME_EPSILON)), 1);
		
	    // Vertex #2: the starting vertex projected to infinity
		VectorXYZW v2 = new VectorXYZW(lightDir, 0);
		
	    // Vertex #3: the ending vertex (just a tiny bit below the original edge)
		if (lightPos.w != 0)
			lightDir = end.subtract(lightPos.xyz().mult(1/lightPos.w)).normalize();
	    
		VectorXYZW v3 = new VectorXYZW(end.add(lightDir.mult(SHADOWVOLUME_EPSILON)), 1);

	    // Vertex #4: the ending vertex projected to infinity
		VectorXYZW v4 = new VectorXYZW(lightDir, 0);
	    
		result.add(v1);
		result.add(v2);
		result.add(v3);
		result.add(v3);
		result.add(v2);
		result.add(v4);
//		result.add(v1);
//		result.add(v3);
//		result.add(v2);
//		result.add(v3);
//		result.add(v4);
//		result.add(v2);
		return result;
	}
	
	/*
	public static final List<VectorXYZW> calculateShadowVolumesPerTriangle(List<VectorXYZ> vertices, VectorXYZW lightPos, FloatBuffer gWVP) {
		
		List<VectorXYZW> shadowVolumes = new ArrayList<VectorXYZW>();
		for (int triangle = 0; triangle < vertices.size()/3; triangle++) {
			VectorXYZ v1 = vertices.get(triangle*3);
			VectorXYZ v2 = vertices.get(triangle*3+1);
			VectorXYZ v3 = vertices.get(triangle*3+2);
			
			VectorXYZ normal = v1.subtract(v2).crossNormalized(v3.subtract(v2));
			
			VectorXYZ lightDir;
			if (lightPos.w == 0)
				lightDir = lightPos.xyz().mult(-1).normalize();
			else
				lightDir = v1.subtract(lightPos.xyz().mult(1/lightPos.w)).normalize();
			
			// Handle only light facing triangles
			if (normal.dot(lightDir) > 0) {
			
				// calculate the volume sides
				shadowVolumes.addAll(emitQuad(v1, v2, lightPos, gWVP));
				shadowVolumes.addAll(emitQuad(v2, v3, lightPos, gWVP));
				shadowVolumes.addAll(emitQuad(v3, v1, lightPos, gWVP));

				// calculate the front/back cap
				VectorXYZW c1 = multMatrixVec(gWVP, v1.add(lightDir.mult(EPSILON)), 1);
				VectorXYZW b1 = multMatrixVec(gWVP, lightDir, 0);

				if (lightPos.w != 0)
					lightDir = v2.subtract(lightPos.xyz().mult(1/lightPos.w)).normalize();
				VectorXYZW c2 = multMatrixVec(gWVP, v2.add(lightDir.mult(EPSILON)), 1);
				VectorXYZW b2 = multMatrixVec(gWVP, lightDir, 0);

				if (lightPos.w != 0)
					lightDir = v3.subtract(lightPos.xyz().mult(1/lightPos.w)).normalize();
				VectorXYZW c3 = multMatrixVec(gWVP, v3.add(lightDir.mult(EPSILON)), 1);
				VectorXYZW b3 = multMatrixVec(gWVP, lightDir, 0);

				shadowVolumes.add(c1);
				shadowVolumes.add(c2);
				shadowVolumes.add(c3);

				// reverse back cap order
				shadowVolumes.add(b1);
				shadowVolumes.add(b3);
				shadowVolumes.add(b2);
			}
			
		}
		
		return shadowVolumes;
	}
	
	private static List<VectorXYZW> emitQuad(VectorXYZ start, VectorXYZ end, VectorXYZW lightPos, FloatBuffer gWVP) {

		List<VectorXYZW> result = new ArrayList<VectorXYZW>(10);
		VectorXYZ lightDir;
		
		// Vertex #1: the starting vertex (just a tiny bit below the original edge)
		if (lightPos.w == 0)
			lightDir = lightPos.xyz().mult(-1).normalize();
		else
			lightDir = start.subtract(lightPos.xyz().mult(1/lightPos.w)).normalize();
		
		VectorXYZW v1 = multMatrixVec(gWVP, start.add(lightDir.mult(EPSILON)), 1);
		
	    // Vertex #2: the starting vertex projected to infinity
		VectorXYZW v2 = multMatrixVec(gWVP, lightDir.mult(EPSILON), 0);
		
	    // Vertex #3: the ending vertex (just a tiny bit below the original edge)
		if (lightPos.w == 0)
			lightDir = lightPos.xyz().mult(-1).normalize();
		else
			lightDir = end.subtract(lightPos.xyz().mult(1/lightPos.w)).normalize();
	    
		VectorXYZW v3 = multMatrixVec(gWVP, end.add(lightDir.mult(EPSILON)), 1);

	    // Vertex #4: the ending vertex projected to infinity
		VectorXYZW v4 = multMatrixVec(gWVP, lightDir.mult(EPSILON), 0); // TODO: correct for directional light?
	    
		result.add(v1);
		result.add(v2);
		result.add(v3);
		result.add(v3);
		result.add(v2);
		result.add(v4);
		return result;
	}
	
	private static VectorXYZW multMatrixVec(FloatBuffer mat, VectorXYZ v, float w) {
		float[] result = new float[4];
		FloatUtil.multMatrixVecf(mat, new float[]{(float)v.x, (float)v.y, (float)v.z, w}, result);
		return new VectorXYZW(result[0], result[1], result[2], result[3]);
	}
	*/
	
}
