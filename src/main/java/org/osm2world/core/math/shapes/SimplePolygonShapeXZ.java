package org.osm2world.core.math.shapes;

import static java.lang.Double.NaN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.AxisAlignedRectangleXZ.bbox;
import static org.osm2world.core.math.SimplePolygonXZ.asSimplePolygon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
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

	/**
	 * returns the number of vertices in this polygon.
	 * The duplicated first/last vertex is <em>not</em> counted twice,
	 * so the result is equivalent to {@link #getVertexListNoDup()}.size().
	 */
	public default int size() {
		return getVertexList().size() - 1;
	}

	/** returns the vertex at a position in the vertex sequence */
	public default VectorXZ getVertex(int index) {
		assert 0 <= index && index < size();
		return getVertexList().get(index);
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

		List<VectorXZ> vertexLoop = getVertexList();

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

	/** creates a new polygon by adding a shift vector to each vector of this */
	public default SimplePolygonShapeXZ shift(VectorXZ shiftVector) {
		return new SimplePolygonXZ(getVertexList().stream().map(shiftVector::add).collect(toList()));
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
			for (VectorXZ v : getVertexList()) {
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

}
