package org.osm2world.core.math.shapes;

import static org.osm2world.core.math.AxisAlignedRectangleXZ.bbox;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IntersectionTestObject;

/**
 * two-dimensional, immutable shape. The shape is not required to be closed.
 * This has a variety of uses, including creating geometries by extrusion.
 */
public interface ShapeXZ extends IntersectionTestObject {

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

}
