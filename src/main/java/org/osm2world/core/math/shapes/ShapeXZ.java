package org.osm2world.core.math.shapes;

import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.AxisAlignedRectangleXZ.bbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.BoundedObject;
import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.VectorXZ;

/**
 * two-dimensional, immutable shape. The shape is not required to be closed.
 * This has a variety of uses, including creating geometries by extrusion.
 */
public interface ShapeXZ extends BoundedObject {

	/**
	 * returns the shape's vertices.
	 * For closed shapes, the first and last vertex are identical.
	 *
	 * @return list of vertices, not empty, not null
	 */
	public List<VectorXZ> vertices();

	/** returns the line segments connecting each successive pair of vertices */
	public default List<LineSegmentXZ> getSegments() {
		List<VectorXZ> vertices = vertices();
		List<LineSegmentXZ> segments = new ArrayList<>(vertices.size() - 1);
		for (int i = 0; i + 1 < vertices.size(); i++) {
			segments.add(new LineSegmentXZ(vertices.get(i), vertices.get(i + 1)));
		}
		return segments;
	}

	@Override
	default AxisAlignedRectangleXZ boundingBox() {
		return bbox(vertices());
	}

	/**
	 * returns a rotated version of this shape. Rotation is around the origin.
	 *
	 * @param angleRad  clockwise rotation angle in radians
	 */
	public default ShapeXZ rotatedCW(double angleRad) {
		List<VectorXZ> rotatedVertices = vertices().stream()
				.map(v -> v.rotate(angleRad))
				.collect(toList());
		return () -> rotatedVertices;
	}

	/**
	 * returns a moved version of this shape
	 */
	public default ShapeXZ shift(VectorXZ moveVector) {
		List<VectorXZ> newVertices = vertices().stream()
				.map(v -> v.add(moveVector))
				.collect(toList());
		return () -> newVertices;
	}

	/** returns all segments of this shape ({@link #getSegments()}) which intersect lineSegment */
	public default Collection<LineSegmentXZ> intersectionSegments(LineSegmentXZ lineSegment) {

		List<LineSegmentXZ> result = new ArrayList<>();

		for (LineSegmentXZ segment : getSegments()) {

			VectorXZ intersection = GeometryUtil.getTrueLineSegmentIntersection(
					lineSegment.p1, lineSegment.p2,
					segment.p1, segment.p2);

			if (intersection != null) {
				result.add(segment);
			}

		}

		return result;

	}

}
