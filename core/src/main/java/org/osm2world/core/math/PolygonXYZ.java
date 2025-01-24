package org.osm2world.core.math;

import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.AxisAlignedRectangleXZ.bbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * a three-dimensional polygon
 */
public class PolygonXYZ implements BoundedObject {

	/** polygon vertices; first and last vertex are equal */
	protected final List<VectorXYZ> vertexLoop;

	/**
	 * @param vertexLoop  vertices defining the polygon;
	 *                  first and last vertex must be equal
	 */
	public PolygonXYZ(List<VectorXYZ> vertexLoop) {

		if (!vertexLoop.get(0).equals(vertexLoop.get(vertexLoop.size() - 1))) {
			throw new IllegalArgumentException("first and last vertex must be equal");
		}

		this.vertexLoop = vertexLoop;

	}

	/**
	 * returns the polygon's vertices. First and last vertex are equal.
	 */
	public List<VectorXYZ> vertices() {
		return vertexLoop;
	}

	/**
	 * returns the polygon's vertices.
	 * Unlike {@link #vertices()}, there is no duplication of the first/last vertex.
	 */
	public List<VectorXYZ> verticesNoDup() {
		return vertexLoop.subList(0, vertexLoop.size() - 1);
	}

	/**
	 * returns the number of vertices in this polygon.
	 * The duplicated first/last vertex is <em>not</em> counted twice,
	 * so the result is equivalent to {@link #verticesNoDup()}.size().
	 */
	public int size() {
		return vertexLoop.size()-1;
	}

	public List<LineSegmentXYZ> getSegments() {
		List<LineSegmentXYZ> segments = new ArrayList<>(vertexLoop.size());
		for (int i = 0; i + 1 < vertexLoop.size(); i++) {
			segments.add(new LineSegmentXYZ(vertexLoop.get(i), vertexLoop.get(i + 1)));
		}
		return segments;
	}

	/**
	 * caller must check whether flattening will result in a simple planar polygon
	 */
	public SimplePolygonXZ getSimpleXZPolygon() {
		List<VectorXZ> verticesXZ = vertexLoop.stream().map(it -> it.xz()).collect(toList());
		return new SimplePolygonXZ(verticesXZ);
	}

	/**
	 * returns a reversed version of this polygon.
	 * It consists of the same vertices, but has the other direction.
	 */
	public PolygonXYZ reverse() {
		List<VectorXYZ> newVertexLoop = new ArrayList<>(vertexLoop);
		Collections.reverse(newVertexLoop);
		return new PolygonXYZ(newVertexLoop);
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return bbox(vertexLoop);
	}

	public PolygonXYZ add(VectorXYZ v) {
		return new PolygonXYZ(vertices().stream().map(v::add).collect(toList()));
	}

	@Override
	public String toString() {
		return vertexLoop.toString();
	}

}