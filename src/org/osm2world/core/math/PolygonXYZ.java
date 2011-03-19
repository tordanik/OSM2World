package org.osm2world.core.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * a three-dimensional polygon 
 */
public class PolygonXYZ {

	/** polygon vertices; first and last vertex are equal */
	private final List<VectorXYZ> vertexLoop;
		
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
	 * returns the polygon's vertices. 
	 * Unlike {@link #getVertexLoop()}, there is no duplication
	 * of the first/last vertex.
	 */
	public List<VectorXYZ> getVertices() {
		return vertexLoop.subList(0, vertexLoop.size()-1);
	}
	
	/** 
	 * returns the polygon's vertices. First and last vertex are equal.
	 */
	public List<VectorXYZ> getVertexLoop() {
		return vertexLoop;
	}
	
	/**
	 * returns the number of vertices in this polygon.
	 * The duplicated first/last vertex is <em>not</em> counted twice,
	 * so the result is equivalent to {@link #getVertices()}.size().
	 */
	public int size() {
		return vertexLoop.size()-1;
	}
	
	//TODO (code duplication): common polygon supertype?
	public List<LineSegmentXYZ> getSegments() {
		List<LineSegmentXYZ> segments = 
			new ArrayList<LineSegmentXYZ>(vertexLoop.size());
		for (int i=0; i+1 < vertexLoop.size(); i++) {
			segments.add(new LineSegmentXYZ(vertexLoop.get(i), vertexLoop.get(i+1)));
		}
		return segments;
	}

	public PolygonXZ getXZPolygon() {
		List<VectorXZ> verticesXZ = new ArrayList<VectorXZ>(vertexLoop.size());
		for (VectorXYZ vertex : vertexLoop) {
			verticesXZ.add(vertex.xz());
		}
		return new PolygonXZ(verticesXZ);
	}

	/**
	 * caller must check whether flattening will result in a simple planar polygon
	 */
	public SimplePolygonXZ getSimpleXZPolygon() {
		List<VectorXZ> verticesXZ = new ArrayList<VectorXZ>(vertexLoop.size());
		for (VectorXYZ vertex : vertexLoop) {
			verticesXZ.add(vertex.xz());
		}
		return new SimplePolygonXZ(verticesXZ);
	}
	
	/**
	 * returns a triangle with the same vertices as this polygon.
	 * Requires that the polygon is triangular!
	 */
	public TriangleXYZ asTriangleXYZ() {
		if (vertexLoop.size() != 4) {
			throw new InvalidGeometryException("attempted creation of triangle " +
					"from polygon with vertex loop of size " + vertexLoop.size() +
					": " + vertexLoop);
		} else {
			return new TriangleXYZ(
					vertexLoop.get(0), 
					vertexLoop.get(1), 
					vertexLoop.get(2));
		}
	}

	/**
	 * returns a reversed version of this polygon.
	 * It consists of the same vertices, but has the other direction.
	 */
	public PolygonXYZ reverse() {
		List<VectorXYZ> newVertexLoop = new ArrayList<VectorXYZ>(vertexLoop);
		Collections.reverse(newVertexLoop);
		return new PolygonXYZ(newVertexLoop);
	}
	
}