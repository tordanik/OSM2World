package org.osm2world.core.math;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class TriangleXYZ {

	public final VectorXYZ v1, v2, v3;

	public TriangleXYZ(VectorXYZ v1, VectorXYZ v2, VectorXYZ v3) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
	}
	
	public List<VectorXYZ> getVertices() {
		return ImmutableList.of(v1, v2, v3);
	}
	
	/**
	 * returns the normalized normal vector of this triangle
	 */
	public VectorXYZ getNormal() {
		//TODO: account for clockwise vs. counterclockwise
		VectorXYZ normal = v2.subtract(v1).cross(v2.subtract(v3));
		return normal.normalize();
	}

	public VectorXYZ getCenter() {
		return new VectorXYZ(
				(v1.x + v2.x + v3.x) / 3,
				(v1.y + v2.y + v3.y) / 3,
				(v1.z + v2.z + v3.z) / 3);
	}
	
	@Override
	public String toString() {
		return "[" + v1.toString() + ", " + v2.toString() + ", " + v3.toString() + "]";
	}
	
}
