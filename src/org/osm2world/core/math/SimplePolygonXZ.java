package org.osm2world.core.math;

import static java.lang.Math.min;
import static org.osm2world.core.math.GeometryUtil.distanceFromLineSegment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.math.shapes.SimpleClosedShapeXZ;

/**
 * a non-self-intersecting polygon in the XZ plane
 */
public class SimplePolygonXZ extends PolygonXZ implements SimpleClosedShapeXZ {
	
	/** stores the signed area */
	private Double signedArea;
	
	/** stores the result for {@link #getArea()} */
	private Double area;

	/** stores the result for {@link #isClockwise()} */
	private Boolean clockwise;
	
	/**
	 * @param vertexLoop  vertices defining the polygon;
	 *                    first and last vertex must be equal
	 * @throws InvalidGeometryException  if the polygon is self-intersecting
	 *                                   or produces invalid area calculation results
	 */
	public SimplePolygonXZ(List<VectorXZ> vertexLoop) {
		
		super(vertexLoop);
		
		assertLoopLength(vertexLoop);
		assertNotSelfIntersecting(vertexLoop);
				
	}

	private void calculateArea() {
		this.signedArea = calculateSignedArea(vertexLoop);
		this.area = Math.abs(signedArea);
		this.clockwise = signedArea < 0;
		
		assertNonzeroArea();
	}
	
	public List<LineSegmentXZ> getSegments() {
		List<LineSegmentXZ> segments = new ArrayList<LineSegmentXZ>(vertexLoop.size());
		for (int i=0; i+1 < vertexLoop.size(); i++) {
			segments.add(new LineSegmentXZ(vertexLoop.get(i), vertexLoop.get(i+1)));
		}
		return segments;
	}
	
	/** returns the polygon's area */
	public double getArea() {
		if (area == null) {
			calculateArea();
		}
		return area;
	}
	
	/** returns the centroid (or "barycenter") of the polygon */
	public VectorXZ getCentroid() {
		
		if (signedArea == null) { calculateArea(); }
		
		double xSum = 0, zSum = 0;
		
		int numVertices = vertexLoop.size() - 1;
		for (int i = 0; i < numVertices; i++) {
			
			double factor = vertexLoop.get(i).x * vertexLoop.get(i+1).z
				- vertexLoop.get(i+1).x * vertexLoop.get(i).z;
			
			xSum += (vertexLoop.get(i).x + vertexLoop.get(i+1).x) * factor;
			zSum += (vertexLoop.get(i).z + vertexLoop.get(i+1).z) * factor;
			
		}
		
		double areaFactor = 1 / (6 * signedArea);
		return new VectorXZ(areaFactor * xSum, areaFactor * zSum);
		
	}

	/**
	 * returns the largest distance between any pair of vertices
	 * of this polygon
	 */
	public double getDiameter() {
		double maxDistance = 0;
		for (int i = 1; i < vertexLoop.size() - 1; i++) {
			for (int j = 0; j < i; j++) {
				double distance = vertexLoop.get(i).distanceTo(vertexLoop.get(j));
				if (distance > maxDistance) {
					maxDistance = distance;
				}
			}
		}
		return maxDistance;
	}
	
	/** returns true if the polygon has clockwise orientation */
	public boolean isClockwise() {
		if (area == null) {
			calculateArea();
		}
		return clockwise;
	}
	
	@Override
	public boolean isSelfIntersecting() {
		return false;
	}

	@Override
	public boolean isSimple() {
		return true;
	}
	
	@Override
	public SimplePolygonXZ asSimplePolygon() {
		return this;
	}

	/**
	 * @return  a {@link PolygonWithHolesXZ}
	 * with this polygon as the outer polygon and no holes
	 */
	public PolygonWithHolesXZ asPolygonWithHolesXZ() {
		return new PolygonWithHolesXZ(this, Collections.<SimplePolygonXZ>emptyList());
	}
	
	/**
	 * returns this polygon if it is counterclockwise,
	 * or the reversed polygon if it is clockwise.
	 */
	public SimplePolygonXZ makeClockwise() {
		return makeRotationSense(true);
	}
	
	/**
	 * returns this polygon if it is clockwise,
	 * or the reversed polygon if it is counterclockwise.
	 */
	public SimplePolygonXZ makeCounterclockwise() {
		return makeRotationSense(false);
	}

	private SimplePolygonXZ makeRotationSense(boolean clockwise) {
		if (this.isClockwise() ^ clockwise) {
			return this.reverse();
		} else {
			return this;
		}
	}
	
	@Override
	public SimplePolygonXZ reverse() {
		List<VectorXZ> newVertexLoop = new ArrayList<VectorXZ>(vertexLoop);
		Collections.reverse(newVertexLoop);
		return new SimplePolygonXZ(newVertexLoop);
	}

	/**
	 * creates a new polygon by adding a shift vector to each vector of this
	 */
	public SimplePolygonXZ shift(VectorXZ shiftVector) {
		List<VectorXZ> newVertexLoop = new ArrayList<VectorXZ>(vertexLoop.size());
		newVertexLoop.add(vertexLoop.get(0).add(shiftVector));
		for (VectorXZ v : vertexLoop) {
			if (!v.equals(vertexLoop.get(0))) {
				newVertexLoop.add(v.add(shiftVector));
			}
		}
		newVertexLoop.add(newVertexLoop.get(0));
		return new SimplePolygonXZ(newVertexLoop);
	}
	
	/**
	 * returns true if the polygon defined by the polygonVertexLoop parameter
	 * contains a given position
	 */
	public static boolean contains(List<VectorXZ> polygonVertexLoop, VectorXZ test) {
	
		assertLoopProperty(polygonVertexLoop);
		
		int i, j;
		boolean c = false;

		for (i = 0, j = polygonVertexLoop.size() - 1; i < polygonVertexLoop.size(); j = i++) {
			if (((polygonVertexLoop.get(i).z > test.z) != (polygonVertexLoop.get(j).z > test.z))
					&& (test.x < (polygonVertexLoop.get(j).x - polygonVertexLoop.get(i).x)
							* (test.z - polygonVertexLoop.get(i).z)
							/ (polygonVertexLoop.get(j).z - polygonVertexLoop.get(i).z) + polygonVertexLoop.get(i).x))
				c = !c;
		}

		return c;
		  
	}
	
	/**
	 * returns true if the polygon contains a given position
	 */
	public boolean contains(VectorXZ test) {
		return SimplePolygonXZ.contains(vertexLoop, test);
	}

	/**
	 * returns true if this polygon contains the parameter polygon
	 */
	public boolean contains(PolygonXZ p) {
		//FIXME: it is possible that a polygon contains all vertices of another polygon, but still not the entire polygon
		for (VectorXZ v : p.getVertices()) {
			if (!vertexLoop.contains(v) && !this.contains(v)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * returns the distance of a point to the segments this polygon.
	 * Note that the distance can be > 0 even if the polygon contains the point
	 */
	public double distanceToSegments(VectorXZ p) {
		double minDistance = Double.MAX_VALUE;
		for (LineSegmentXZ s : getSegments()) {
			minDistance = min(minDistance, distanceFromLineSegment(p, s));
		}
		return minDistance;
	}
	
	/**
	 * returns a different polygon that is constructed from this polygon
	 * by removing all vertices where this has an angle close to 180°
	 * (i.e. where removing the vertex does not change the polygon very much).
	 */
	public SimplePolygonXZ getSimplifiedPolygon() {
		
		boolean[] delete = new boolean[size()];
		int deleteCount = 0;
		
		for (int i = 0; i < size(); i++) {
			VectorXZ segmentBefore = getVertex(i).subtract(getVertexBefore(i));
			VectorXZ segmentAfter = getVertexAfter(i).subtract(getVertex(i));
			double dot = segmentBefore.normalize().dot(segmentAfter.normalize());
			if (Math.abs(dot - 1) < 0.05) {
				delete[i] = true;
				deleteCount += 1;
			}
		}
		
		if (deleteCount == 0 || deleteCount > size() - 3) {
			return this;
		} else {
			
			List<VectorXZ> newVertexList = new ArrayList<VectorXZ>(getVertices());
			
			//iterate backwards => it doesn't matter when higher indices change
			for (int i = size() - 1; i >= 0; i--) {
				if (delete[i]) {
					newVertexList.remove(i);
				}
			}
			
			newVertexList.add(newVertexList.get(0));
			return new SimplePolygonXZ(newVertexList);
			
		}
		
	}
	
	@Override
	public Collection<TriangleXZ> getTriangulation() {
		return TriangulationUtil.triangulate(this, Collections.<SimplePolygonXZ>emptyList());
	}
	
	/**
	 * calculates the area of a planar non-self-intersecting polygon.
	 * The result is negative if the polygon is clockwise.
	 */
	private static double calculateSignedArea(List<VectorXZ> vertexLoop) {
				
		double sum = 0f;
		
		for (int i = 0; i + 1 < vertexLoop.size(); i++) {
			sum += vertexLoop.get(i).x * vertexLoop.get(i+1).z;
			sum -= vertexLoop.get(i+1).x * vertexLoop.get(i).z;
		}
		
		return sum / 2;
		
	}

	/**
	 * @throws InvalidGeometryException  if the vertex loop is too short
	 */
	private static void assertLoopLength(List<VectorXZ> vertexLoop) {
		if (vertexLoop.size() <= 3) {
			throw new InvalidGeometryException(
					"polygon needs more than 2 vertices\n"
					+ "Polygon vertex loop: " + vertexLoop);
		}
	}

	/**
	 * @throws InvalidGeometryException  if the vertex loop is self-intersecting
	 */
	private static void assertNotSelfIntersecting(List<VectorXZ> vertexLoop) {
		if (isSelfIntersecting(vertexLoop)) {
			throw new InvalidGeometryException(
					"polygon must not be self-intersecting\n"
					+ "Polygon vertices: " + vertexLoop);
		}
	}
	
	/**
	 * @throws InvalidGeometryException  if area is 0
	 */
	private void assertNonzeroArea() {
		if (area == 0) {
			throw new InvalidGeometryException(
					"a polygon's area must be positive, but it's "
					+ area + " for this polygon.\nThis problem can be caused "
					+ "by broken polygon data or imprecise calculations"
					+ "\nPolygon vertices: " + vertexLoop);
		}
	}
	
}