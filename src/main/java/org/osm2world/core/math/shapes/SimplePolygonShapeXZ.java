package org.osm2world.core.math.shapes;

import static java.lang.Double.NaN;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.AxisAlignedRectangleXZ.bbox;
import static org.osm2world.core.math.SimplePolygonXZ.asSimplePolygon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;

public interface SimplePolygonShapeXZ extends SimpleClosedShapeXZ, PolygonShapeXZ {

	/** returns the shape's outer ring. As this is already a shape without holes, it just returns the shape itself. */
	@Override
	default SimplePolygonShapeXZ getOuter() {
		return this;
	}

	/** returns the shape's holes. As this is a simple shape, the result will be empty. */
	@Override
	default Collection<? extends SimplePolygonShapeXZ> getHoles() {
		return emptyList();
	}

	@Override
	public default List<SimplePolygonShapeXZ> getRings() {
		return singletonList(this);
	}

	/**
	 * returns the number of vertices in this polygon.
	 * The duplicated first/last vertex is <em>not</em> counted twice,
	 * so the result is equivalent to {@link #verticesNoDup()}.size().
	 */
	public default int size() {
		return vertices().size() - 1;
	}

	/** returns the vertex at a position in the vertex sequence */
	public default VectorXZ getVertex(int index) {
		assert 0 <= index && index < size();
		return vertices().get(index);
	}

	/**
	 * returns the successor of the vertex at a position in the vertex sequence.
	 * This wraps around the vertex loop, so the successor of the last vertex
	 * is the first vertex.
	 */
	public default VectorXZ getVertexAfter(int index) {
		assert 0 <= index && index < size();
		return getVertex((index + 1) % size());
	}

	/**
	 * returns the predecessor of the vertex at a position in the vertex sequence.
	 * This wraps around the vertex loop, so the predecessor of the first vertex
	 * is the last vertex.
	 */
	public default VectorXZ getVertexBefore(int index) {
		assert 0 <= index && index < size();
		return getVertex((index + size() - 1) % size());
	}

	/** returns true if the polygon contains a given position */
	@Override
	public default boolean contains(VectorXZ v) {

		List<VectorXZ> vertexLoop = vertices();

		int i, j;
		boolean c = false;

		for (i = 0, j = vertexLoop.size() - 1; i < vertexLoop.size(); j = i++) {
			if (((vertexLoop.get(i).z > v.z) != (vertexLoop.get(j).z > v.z))
					&& (v.x < (vertexLoop.get(j).x - vertexLoop.get(i).x)
							* (v.z - vertexLoop.get(i).z)
							/ (vertexLoop.get(j).z - vertexLoop.get(i).z) + vertexLoop.get(i).x))
				c = !c;
		}

		return c;

	}

	public default boolean intersects(VectorXZ segmentP1, VectorXZ segmentP2) {

		List<VectorXZ> vertexList = vertices();

		for (int i = 0; i + 1 < vertexList.size(); i++) {

			VectorXZ intersection = GeometryUtil.getTrueLineSegmentIntersection(
					segmentP1, segmentP2,
					vertexList.get(i), vertexList.get(i+1));

			if (intersection != null) {
				return true;
			}

		}

		return false;

	}

	/** @see #intersects(VectorXZ, VectorXZ) */
	public default boolean intersects(LineSegmentXZ lineSegment) {
		return intersects(lineSegment.p1, lineSegment.p2);
	}

	@Override
	public default List<VectorXZ> intersectionPositions(LineSegmentXZ lineSegment) {

		List<VectorXZ> result = new ArrayList<>();
		List<VectorXZ> vertexLoop = vertices();

		for (int i = 0; i + 1 < vertexLoop.size(); i++) {

			VectorXZ intersection = GeometryUtil.getTrueLineSegmentIntersection(
					lineSegment.p1, lineSegment.p2,
					vertexLoop.get(i), vertexLoop.get(i+1));

			if (intersection != null) {
				result.add(intersection);
			}

		}

		return result;

	}

	@Override
	public default Collection<VectorXZ> intersectionPositions(PolygonShapeXZ p2) {
		List<VectorXZ> intersectionPositions = new ArrayList<>();
		for (SimplePolygonShapeXZ ring : p2.getRings()) {
			for (LineSegmentXZ lineSegment : ring.getSegments()) {
				intersectionPositions.addAll(this.intersectionPositions(lineSegment));
			}
		}
		return intersectionPositions;
	}

	/** returns this polygon's area */
	@Override
	public default double getArea() {
		return asSimplePolygon(this).getArea();
	}

	/** returns the centroid (or "barycenter") of the polygon */
	public default VectorXZ getCentroid() {
		return asSimplePolygon(this).getCentroid();
	}

	@Override
	public default double getDiameter() {
		double maxDistance = 0;
		for (int i = 1; i < size(); i++) {
			for (int j = 0; j < i; j++) {
				double distance = getVertex(i).distanceTo(getVertex(j));
				if (distance > maxDistance) {
					maxDistance = distance;
				}
			}
		}
		return maxDistance;
	}

	/**
	 * returns the convex hull of this polygon.
	 *
	 * @return  the convex hull. Its points are ordered as they were in the original polygon.
	 */
	public default SimplePolygonShapeXZ convexHull() {
		return asSimplePolygon(this).convexHull();
	}

	/**
	 * Calculates the smallest possible bounding box for this polygon.
	 * The result is not (generally) an axis-aligned bounding box!
	 *
	 * Relies on the fact that one side of the box must be collinear with
	 * one of the sides of the polygon's convex hull.
	 *
	 * @return  a simple polygon with exactly 4 vertices, representing the box
	 */
	public default SimplePolygonXZ minimumRotatedBoundingBox() {

		/*
		 * For each side of the polygon, rotate the polygon to make that side
		 * parallel to the Z axis, then calculate the axis aligned bounding box.
		 * These are the candidate boxes for minimum area.
		 */

		AxisAlignedRectangleXZ minBox = null;
		double angleForMinBox = NaN;

		for (int i = 0; i < this.size(); i++) {

			double angle = getVertex(i).angleTo(this.getVertexAfter(i));

			List<VectorXZ> rotatedVertices = new ArrayList<VectorXZ>();
			for (VectorXZ v : vertices()) {
				rotatedVertices.add(v.rotate(-angle));
			}

			AxisAlignedRectangleXZ box = bbox(rotatedVertices);

			if (minBox == null || box.area() < minBox.area()) {
				minBox = box;
				angleForMinBox = angle;
			}

		}

		/* construct the result */

		return new SimplePolygonXZ(asList(
				minBox.bottomLeft().rotate(angleForMinBox),
				minBox.bottomRight().rotate(angleForMinBox),
				minBox.topRight().rotate(angleForMinBox),
				minBox.topLeft().rotate(angleForMinBox),
				minBox.bottomLeft().rotate(angleForMinBox)));

	}

	@Override
	default SimplePolygonShapeXZ rotatedCW(double angleRad) {
		List<VectorXZ> rotatedVertexList = vertices().stream()
				.map(v -> v.rotate(angleRad))
				.collect(toList());
		return new SimplePolygonXZ(rotatedVertexList);
	}

	/** creates a new polygon by adding a shift vector to each vector of this */
	@Override
	public default SimplePolygonShapeXZ shift(VectorXZ moveVector) {
		return new SimplePolygonXZ(vertices().stream().map(moveVector::add).collect(toList()));
	}

}
