package org.osm2world.core.math.shapes;

import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.AxisAlignedRectangleXZ.bbox;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.BoundedObject;
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
	 * TODO support accuracy-dependent vertex collections (e.g. for circles)
	 *
	 * @return list of vertices, not empty, not null
	 */
	public List<VectorXZ> getVertexList();

	/** returns the line segments connecting each successive pair of vertices */
	public default List<LineSegmentXZ> getSegments() {
		List<VectorXZ> vertexList = getVertexList();
		List<LineSegmentXZ> segments = new ArrayList<>(vertexList.size() - 1);
		for (int i = 0; i + 1 < vertexList.size(); i++) {
			segments.add(new LineSegmentXZ(vertexList.get(i), vertexList.get(i + 1)));
		}
		return segments;
	}

	@Override
	default AxisAlignedRectangleXZ boundingBox() {
		return bbox(getVertexList());
	}

	/**
	 * returns a rotated version of this shape. Rotation is around the origin.
	 *
	 * @param angleRad  clockwise rotation angle in radians
	 */
	public default ShapeXZ rotatedCW(double angleRad) {
		List<VectorXZ> rotatedVertexList = getVertexList().stream()
				.map(v -> v.rotate(angleRad))
				.collect(toList());
		return () -> rotatedVertexList;
	}

}
