package org.osm2world.core.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * a non-self-intersecting polygon in the XZ plane
 */
public class SimplePolygonXZ extends PolygonXZ {
	
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

	private void calculateArea(List<VectorXZ> vertexLoop) {
		double signedArea = calculateSignedArea(vertexLoop);
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
			calculateArea(vertexLoop);
		}
		return area;
	}
	
	/** returns true if the polygon has clockwise orientation */
	public boolean isClockwise() {
		if (area == null) {
			calculateArea(vertexLoop);
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
	 * returns true if the polygon defined by the polygonVertexLoop parameter
	 * contains a given position
	 */
	public static boolean contains(List<VectorXZ> polygonVertexLoop, VectorXZ test) {
	
		assertLoopProperty(polygonVertexLoop);
		
		// uses algorithm from
		// http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
		// TODO: contact author
		
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
		for (VectorXZ v : p.getVertices()) {
			if (!this.contains(v)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * calculates the area of a planar non-self-intersecting polygon.
	 * This isn't supposed to deal with projection issues.
	 * The result is negative if the polygon is clockwise.
	 */
	private static double calculateSignedArea(List<VectorXZ> vertices) {
		
		//TODO: find source of algorithm
		
		double area = 0f;
			
		for (int i = 0; i + 1 < vertices.size(); i++) {
			area += vertices.get(i).x * vertices.get(i+1).z;
			area -= vertices.get(i+1).x * vertices.get(i).z;
		}
		
		return area;
		
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