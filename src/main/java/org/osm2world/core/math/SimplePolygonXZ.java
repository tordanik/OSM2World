package org.osm2world.core.math;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.*;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.GeometryUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.math.shapes.SimpleClosedShapeXZ;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;

/**
 * a non-self-intersecting polygon in the XZ plane
 */
public class SimplePolygonXZ implements SimplePolygonShapeXZ {

	/** polygon vertices; first and last vertex are equal */
	protected final List<VectorXZ> vertexLoop;

	/** stores the signed area */
	private Double signedArea;

	/** stores the result for {@link #getArea()} */
	private Double area;

	/** stores the result for {@link #isClockwise()} */
	private Boolean clockwise;

	/**
	 * @param vertexLoop  vertices defining the polygon; first and last vertex must be equal.
	 * @throws InvalidGeometryException  if the polygon self-intersects or produces invalid area calculation results
	 */
	public SimplePolygonXZ(List<VectorXZ> vertexLoop) {

		this.vertexLoop = vertexLoop;

		assertLoopProperty(vertexLoop);
		assertLoopLength(vertexLoop);
		assertNotSelfIntersecting(vertexLoop);
		assertNoDuplicates(vertexLoop);

	}

	public static final SimplePolygonXZ asSimplePolygon(SimpleClosedShapeXZ shape) {
		if (shape instanceof SimplePolygonXZ) {
			return (SimplePolygonXZ) shape;
		} else {
			return new SimplePolygonXZ(shape.vertices());
		}
	}

	/**
	 * returns the number of vertices in this polygon.
	 * The duplicated first/last vertex is <em>not</em> counted twice,
	 * so the result is equivalent to {@link #getVertices()}.size().
	 */
	@Override
	public int size() {
		return vertexLoop.size()-1;
	}

	/**
	 * returns the polygon's vertices.
	 * Unlike {@link #vertices()}, there is no duplication
	 * of the first/last vertex.
	 */
	public List<VectorXZ> getVertices() {
		return vertexLoop.subList(0, vertexLoop.size()-1);
	}

	/**
	 * returns the polygon's vertices. First and last vertex are equal.
	 *
	 * @return list of vertices, not empty, not null
	 */
	@Override
	public List<VectorXZ> vertices() {
		return vertexLoop;
	}

	/**
	 * returns a collection that contains all vertices of this polygon
	 * at least once. Can be used if you don't care about whether the first/last
	 * vector is duplicated.
	 */
	public List<VectorXZ> getVertexCollection() {
		return vertexLoop;
	}

	/**
	 * returns the polygon segment with minimum distance to a given point
	 */
	public LineSegmentXZ getClosestSegment(VectorXZ point) {

		LineSegmentXZ closestSegment = null;
		double closestDistance = Double.MAX_VALUE;

		for (LineSegmentXZ segment : getSegments()) {
			double distance = distanceFromLineSegment(point, segment);
			if (distance < closestDistance) {
				closestSegment = segment;
				closestDistance = distance;
			}
		}

		return closestSegment;

	}

	/**
	 * returns the point on this polygon that is closest to a given point.
	 * For points within this polygon, this returns the point itself.
	 */
	public VectorXZ closestPoint(VectorXZ point) {
		if (this.contains(point)) {
			return point;
		} else {
			return getClosestSegment(point).closestPoint(point);
		}
	}

	private void calculateArea() {
		this.signedArea = calculateSignedArea(vertexLoop);
		this.area = Math.abs(signedArea);
		this.clockwise = signedArea < 0;

		assertNonzeroArea();
	}

	@Override
	public double getArea() {
		if (area == null) {
			calculateArea();
		}
		return area;
	}

	/**
	 * returns true if the other polygon has the same vertices in the same order,
	 * possibly with a different start vertex
	 */
	public boolean isEquivalentTo(SimplePolygonXZ other) {

		if (vertexLoop.size() != other.vertexLoop.size()) {
			return false;
		}

		List<VectorXZ> ownVertices = getVertices();
		List<VectorXZ> otherVertices = other.getVertices();

		for (int offset = 0; offset < ownVertices.size(); offset ++) {

			boolean matches = true;

			for (int i = 0; i < ownVertices.size(); i++) {
				int iWithOffset = (i + offset) % ownVertices.size();
				if (!otherVertices.get(i).equals(ownVertices.get(iWithOffset))) {
					matches = false;
					break;
				}
			}

			if (matches) {
				return true;
			}

		}

		return false;

	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof SimplePolygonXZ) {
			return vertexLoop.equals(((SimplePolygonXZ)obj).vertexLoop);
		} else {
			return false;
		}

	}

	@Override
	public int hashCode() {
		return vertexLoop.hashCode();
	}

	/** returns the centroid (or "barycenter") of the polygon */
	@Override
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

	/** returns true if the polygon has clockwise orientation */
	@Override
	public boolean isClockwise() {
		if (area == null) {
			calculateArea();
		}
		return clockwise;
	}


	/**
	 * returns a triangle with the same vertices as this polygon.
	 * Requires that the polygon is triangular!
	 */
	public TriangleXZ asTriangleXZ() {
		if (vertexLoop.size() != 4) {
			throw new InvalidGeometryException("attempted creation of triangle " +
					"from polygon with vertex loop of size " + vertexLoop.size() +
					": " + vertexLoop);
		} else {
			return new TriangleXZ(
					vertexLoop.get(0),
					vertexLoop.get(1),
					vertexLoop.get(2));
		}
	}

	public PolygonXYZ xyz(final double y) {
		return new PolygonXYZ(VectorXZ.listXYZ(vertexLoop, y));
	}

	/**
	 * returns the average of all vertex coordinates.
	 * The result is not necessarily contained by this polygon.
	 */
	public VectorXZ getCenter() {
		double x=0, z=0;
		int numberVertices = vertexLoop.size()-1;
		for (VectorXZ vertex : getVertices()) {
			x += vertex.x / numberVertices;
			z += vertex.z / numberVertices;
			/* single division per coordinate after loop would be faster,
			 * but might cause numbers to get too large */
		}
		return new VectorXZ(x, z);
	}

	/**
	 * returns the length of the polygon's outline.
	 * (This does <em>not</em> return the number of vertices,
	 * but the sum of distances between subsequent nodes.)
	 */
	public double getOutlineLength() {
		double length = 0;
		for (int i = 0; i+1 < vertexLoop.size(); i++) {
			length += VectorXZ.distance(vertexLoop.get(i), vertexLoop.get(i+1));
		}
		return length;
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

	public SimplePolygonXZ reverse() {
		List<VectorXZ> newVertexLoop = new ArrayList<VectorXZ>(vertexLoop);
		Collections.reverse(newVertexLoop);
		return new SimplePolygonXZ(newVertexLoop);
	}

	@Override
	public SimplePolygonXZ transform(Function<VectorXZ, VectorXZ> operation) {
		return new SimplePolygonXZ(vertices().stream().map(operation).collect(toList())).makeRotationSense(this.isClockwise());
	}

	/**
	 * returns the distance of a point to the segments this polygon.
	 * Note that the distance can be greater than 0 even if the polygon contains the point
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

		SimplePolygonXZ result = getSimplifiedPolygon(0.05);

		if (result == null || abs(result.getArea() - this.getArea()) / this.getArea() > 0.1) {
			// redo with less tolerance
			result = getSimplifiedPolygon(0.001);
		}

		return result != null ? result : this;

	}

	/**
	 * simple implementation of {@link #getSimplifiedPolygon()} that can produce invalid polygons
	 *
	 * @return the simplified polygon, or null if the result would be invalid
	 */
	private @Nullable SimplePolygonXZ getSimplifiedPolygon(double maxDotProduct) {

		boolean[] delete = new boolean[size()];
		int deleteCount = 0;

		for (int i = 0; i < size(); i++) {
			VectorXZ segmentBefore = getVertex(i).subtract(getVertexBefore(i));
			VectorXZ segmentAfter = getVertexAfter(i).subtract(getVertex(i));
			double dot = segmentBefore.normalize().dot(segmentAfter.normalize());
			if (abs(dot - 1) < maxDotProduct) {
				delete[i] = true;
				deleteCount += 1;
			}
		}

		if (deleteCount == 0 || deleteCount > size() - 3) {
			return this;
		} else {

			List<VectorXZ> newVertexList = new ArrayList<>(getVertices());

			//iterate backwards => it doesn't matter when higher indices change
			for (int i = size() - 1; i >= 0; i--) {
				if (delete[i]) {
					newVertexList.remove(i);
				}
			}

			newVertexList.add(newVertexList.get(0));

			try {
				return new SimplePolygonXZ(newVertexList);
			} catch (InvalidGeometryException e) {
				return null;
			}

		}

	}

	@Override
	public List<TriangleXZ> getTriangulation() {

		List<TriangleXZ> result = TriangulationUtil.triangulate(this, emptyList());

		//ensure that the triangles have the same winding as this shape
		for (int i = 0; i < result.size(); i++) {
			if (this.isClockwise()) {
				result.set(i, result.get(i).makeClockwise());
			} else {
				result.set(i, result.get(i).makeCounterclockwise());
			}
		}

		return result;

	}

	@Override
	public final SimplePolygonXZ convexHull() {

		List<VectorXZ> vertices = this.makeClockwise().getVertices();

		/* determine points with min/max x value (guaranteed to be in convex hull) */

		VectorXZ minV = Collections.min(vertices, comparingDouble(v -> v.x));
		VectorXZ maxV = Collections.max(vertices, comparingDouble(v -> v.x));

		int minI = vertices.indexOf(minV);
		int maxI = vertices.indexOf(maxV);

		/* split the polygon into an upper and lower "half" at the two points */

		List<VectorXZ> upperHalf = new ArrayList<>();
		List<VectorXZ> lowerHalf = new ArrayList<>();

		upperHalf.add(minV);

		for (int i = (minI + 1) % vertices.size(); i != maxI; i = (i+1) % vertices.size()) {
			upperHalf.add(vertices.get(i));
		}

		upperHalf.add(maxV);

		lowerHalf.add(maxV);

		for (int i = (maxI + 1) % vertices.size(); i != minI; i = (i+1) % vertices.size()) {
			lowerHalf.add(vertices.get(i));
		}

		lowerHalf.add(minV);

		/* perform the calculation for each of the two parts */

		List<VectorXZ> upperResult = convexHullPart(upperHalf);
		List<VectorXZ> lowerResult = convexHullPart(lowerHalf);

		/* combine the results */

		upperResult.addAll(lowerResult.subList(1, lowerResult.size()));

		if (!this.isClockwise()) {
			Collections.reverse(upperResult);
		}

		return new SimplePolygonXZ(upperResult);

	}

	/**
	 * calculates the convex hull partially for the upper or lower "half"
	 * of a polygon. Used in {@link #convexHull()}.
	 */
	private static List<VectorXZ> convexHullPart(List<VectorXZ> vertices) {

		checkArgument(vertices.size() >= 2);

		if (vertices.size() < 3) {
			return vertices;
		}

		// preliminary result, vertices can be removed from its end at a later point
		List<VectorXZ> result = new ArrayList<VectorXZ>();

		result.add(vertices.get(0));
		result.add(vertices.get(1));

		for (int i = 2; i < vertices.size(); i++) {

			VectorXZ v = vertices.get(i);

			while (result.size() > 1) {

				if (isRightOf(result.get(result.size() - 2), v,
						result.get(result.size() - 1))) {

					result.remove(result.size() - 1);

				} else {
					break;
				}

			}

			result.add(v);

		}

		return result;

	}


	private static boolean handleStartEvent(TreeSet<LineSegmentXZ> sweepLine, LineSegmentXZ line) {
		LineSegmentXZ lower = sweepLine.lower(line);
		LineSegmentXZ higher = sweepLine.higher(line);

		sweepLine.add(line);

		if (lower != null && lower.intersects(line.p1, line.p2)) {
			return true;
		}

		if (higher != null && higher.intersects(line.p1, line.p2)) {
			return true;
		}
		return false;
	}

	private static boolean handleEndEvent(TreeSet<LineSegmentXZ> sweepLine, LineSegmentXZ line) {
		LineSegmentXZ lower = sweepLine.lower(line);
		LineSegmentXZ higher = sweepLine.higher(line);

		sweepLine.remove(line);

		if ((lower == null) || (higher == null)) {
            return false;
        }

		if (lower.intersects(higher.p1, higher.p2)) {
			return true;
		}
		return false;
	}


	@Override
	public String toString() {
		return vertexLoop.toString();
	}

	/**
	 * returns true if the polygon defined by the polygonVertexLoop parameter
	 * is self-intersecting.<br>
	 * The Code is based on Shamos-Hoey's algorithm
	 *
	 * TODO: if the end vertex of two line segments are the same the
	 *       polygon is never considered as self intersecting on purpose.
	 *       This behavior should probably be reconsidered, but currently
	 *       left as is due to frequent cases of such polygons.
	 */
	public static boolean isSelfIntersecting(List<VectorXZ> polygonVertexLoop) {

		final class Event {
			boolean start;
			LineSegmentXZ line;

			Event(LineSegmentXZ l, boolean s) {
				this.line = l;
				this.start = s;
			}
		}

		// we have n-1 vertices as the first and last vertex are the same
		final int segments = polygonVertexLoop.size()-1;

		// generate an array of input events associated with their line segments
		Event[] events = new Event[segments*2];
		for (int i = 0; i < segments; i++) {
			VectorXZ v1 = polygonVertexLoop.get(i);
			VectorXZ v2 = polygonVertexLoop.get(i+1);

			// Create a line where the first vertex is left (or above) the second vertex
			LineSegmentXZ line;
			if ((v1.x < v2.x) || ((v1.x == v2.x) && (v1.z < v2.z))) {
				line = new LineSegmentXZ(v1, v2);
			} else {
				line = new LineSegmentXZ(v2, v1);
			}

			events[2*i] = new Event(line, true);
			events[2*i+1] = new Event(line, false);
		}

		// sort the input events according to the x-coordinate, then z-coordinate
		Arrays.sort(events, (Event e1, Event e2) -> {

				VectorXZ v1 = e1.start? e1.line.p1 : e1.line.p2;
				VectorXZ v2 = e2.start? e2.line.p1 : e2.line.p2;

				if (v1.x < v2.x) return -1;
				else if (v1.x == v2.x) {
					if (v1.z < v2.z) return -1;
					else if (v1.z == v2.z) return 0;
				}
				return 1;
			});

		// A TreeSet, used for the sweepline algorithm
		TreeSet<LineSegmentXZ> sweepLine = new TreeSet<LineSegmentXZ>((LineSegmentXZ l1, LineSegmentXZ l2) -> {

				VectorXZ v1 = l1.p1;
				VectorXZ v2 = l2.p1;

				if (v1.z < v2.z) return -1;
				else if (v1.z == v2.z) {
					if (v1.x < v2.x) return -1;
					else if (v1.x == v2.x) {
						if (l1.p2.z < l2.p2.z) return -1;
						else if (l1.p2.z == l2.p2.z) {
							if (l1.p2.x < l2.p2.x) return -1;
							else if (l1.p2.x == l2.p2.x) return 0;
						}
					}
				}
				return 1;
			});

		// start the algorithm by visiting every event
		for (Event event : events) {
			LineSegmentXZ line = event.line;
			if (event.start) {
				handleStartEvent(sweepLine, line);
			} else {
				handleEndEvent(sweepLine, line);
			}
		}
		return false;
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
	 * checks that the first and last vertex of the vertex list are equal.
	 * @throws IllegalArgumentException  if first and last vertex aren't equal.
	 * (This is usually a programming error, therefore InvalidGeometryException is not used.)
	 */
	protected static void assertLoopProperty(List<VectorXZ> vertexLoop) {
		if (!vertexLoop.get(0).equals(vertexLoop.get(vertexLoop.size() - 1))) {
			throw new IllegalArgumentException("first and last vertex must be equal\n"
					+ "Polygon vertices: " + vertexLoop);
		}
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
	 * @throws InvalidGeometryException  if the vertex loop has duplicated points
	 */
	private static void assertNoDuplicates(List<VectorXZ> vertexLoop) {
		for (int i = 0; i < vertexLoop.size() - 1; i++) {
			if (vertexLoop.get(i + 1).distanceTo(vertexLoop.get(i)) == 0) {
				throw new InvalidGeometryException(
						"polygon must not not have duplicate points\n"
						+ "Polygon vertices: " + vertexLoop);
			}
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