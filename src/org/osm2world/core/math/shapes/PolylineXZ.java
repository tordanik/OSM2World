package org.osm2world.core.math.shapes;

import static java.util.Arrays.asList;

import java.util.List;

import org.osm2world.core.math.VectorXZ;

/**
 * a polygon (aka linestring)
 */
public class PolylineXZ implements ShapeXZ {
	
	private final List<VectorXZ> vertices;
	
	public PolylineXZ(List<VectorXZ> vertices) {
		this.vertices = vertices;
	}
	
	public PolylineXZ(VectorXZ... vertices) {
		this(asList(vertices));
	}
	
	@Override
	public List<VectorXZ> getVertexList() {
		return vertices;
	}
	
}
