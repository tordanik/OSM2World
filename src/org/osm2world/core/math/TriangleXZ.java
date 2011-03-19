package org.osm2world.core.math;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class TriangleXZ {

	public final VectorXZ v1, v2, v3;

	public TriangleXZ(VectorXZ v1, VectorXZ v2, VectorXZ v3) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
	}
	
	public List<VectorXZ> getVertices() {
		return ImmutableList.of(v1, v2, v3);
	}

	public VectorXZ getCenter() {
		return new VectorXZ(
				(v1.x + v2.x + v3.x) / 3,
				(v1.z + v2.z + v3.z) / 3);
	}
	
	public boolean contains(VectorXZ point) {
		return SimplePolygonXZ.contains(Arrays.asList(v1, v2, v3, v1), point); //TODO: avoid creating new lists?
	}
	
	public TriangleXYZ xyz(double y) {
		return new TriangleXYZ(v1.xyz(y), v2.xyz(y), v3.xyz(y));
	}
	
	@Override
	public String toString() {
		return "[" + v1.toString() + ", " + v2.toString() + ", " + v3.toString() + "]";
	}

	public boolean isClockwise() {
		return GeometryUtil.isRightOf(v3, v1, v2);
	}
	
	/**
	 * returns this triangle if it is counterclockwise,
	 * or the reversed triangle if it is clockwise.
	 */
	public TriangleXZ makeClockwise() {
		return makeRotationSense(true);
	}
	
	/**
	 * returns this triangle if it is clockwise,
	 * or the reversed triangle if it is counterclockwise.
	 */
	public TriangleXZ makeCounterclockwise() {
		return makeRotationSense(false);
	}

	private TriangleXZ makeRotationSense(boolean clockwise) {
		if (isClockwise() ^ clockwise) {
			return this.reverse();
		} else {
			return this;
		}
	}
	
	/**
	 * returns an inversed version of this triangle.
	 * It consists of the same vertices, but has the other direction.
	 */
	public TriangleXZ reverse() {
		return new TriangleXZ(v3, v2, v1);
	}
	
}
