package org.osm2world.core.math;
import org.osm2world.core.math.PolygonXZ;
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
public class SimplePolygonXZ extends PolygonXZ implements SimplePolygonShapeXZ {

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
		this.vertexLoop = new ArrayList<>(vertexLoop);
		validatePolygon();
	}

	public static final SimplePolygonXZ asSimplePolygon(SimpleClosedShapeXZ shape) {
		if (shape instanceof SimplePolygonXZ) {
			return (SimplePolygonXZ) shape;
		} else {
			return new SimplePolygonXZ(shape.vertices());
		}
	}

	private void validatePolygon() {
		if (vertexLoop.size() < 4 || !vertexLoop.get(0).equals(vertexLoop.get(vertexLoop.size() - 1))) {
			throw new IllegalArgumentException("Polygon must be closed and have at least 3 distinct vertices.");
		}
		if (PolygonUtils.isSelfIntersecting(vertexLoop)) {
			throw new InvalidGeometryException("Polygon must not be self-intersecting.");
		}
	}

	@Override
	public List<VectorXZ> vertices() {
		return vertexLoop.subList(0, vertexLoop.size() - 1);
	}

	@Override
	public double getArea() {
		return Math.abs(PolygonUtils.calculateSignedArea(vertexLoop));
	}

	@Override
	public boolean isClockwise() {
		return PolygonUtils.isClockwise(vertexLoop);
	}

	@Override
	public VectorXZ getCentroid() {
		return PolygonUtils.calculateCentroid(vertexLoop);
	}

	@Override
	public List<TriangleXZ> getTriangulation() {
		return TriangulationUtil.triangulate(this, emptyList());
	}

	@Override
	public int size() {
		return vertexLoop.size()-1;
	}

	/**
	 * returns the polygon's vertices.
	 * Unlike {@link #vertices()}, there is no duplication
	 * of the first/last vertex.
	 */
	@Override
	public List<VectorXZ> getVertices() {
		return vertexLoop.subList(0, vertexLoop.size()-1);
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

    @Override
	protected SimplePolygonXZ makeRotationSense(boolean clockwise) {
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


	@Override
	public String toString() {
		return vertexLoop.toString();
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
		if (PolygonUtils.isSelfIntersecting(vertexLoop)) {
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