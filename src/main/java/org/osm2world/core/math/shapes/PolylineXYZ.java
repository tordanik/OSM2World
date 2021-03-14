package org.osm2world.core.math.shapes;

import java.util.List;
import java.util.Objects;

import org.osm2world.core.math.VectorXYZ;

public class PolylineXYZ {

	protected final List<VectorXYZ> vertices;

	public PolylineXYZ(List<VectorXYZ> vertices) {
		this.vertices = vertices;
	}

	public List<VectorXYZ> getVertices() {
		return vertices;
	}

	public int size() {
		return vertices.size();
	}

	public double length() {
		double result = 0;
		for (int i = 0; i + 1 < vertices.size(); i++) {
			result += vertices.get(i).distanceTo(vertices.get(i+1));
		}
		return result;
	}

	@Override
	public int hashCode() {
		return vertices.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PolylineXYZ && Objects.equals(vertices, ((PolylineXYZ)obj).vertices);
	}

	@Override
	public String toString() {
		return vertices.toString();
	}

}
