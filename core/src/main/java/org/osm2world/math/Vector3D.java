package org.osm2world.math;

public interface Vector3D extends BoundedObject {

	public double getX();
	public double getY();
	public double getZ();

	public VectorXZ xz();

	default VectorXYZ xyz() {
		if (this instanceof VectorXYZ) {
			return (VectorXYZ) this;
		} else {
			return new VectorXYZ(getX(), getY(), getZ());
		}
	}

	public static double distance(Vector3D v1, Vector3D v2) {
		return v1.xyz().distanceTo(v2.xyz());
	}

	/**
	 * returns a vector with all negative zero (-0.0) components replaced with positive zero (0.0).
	 * This avoids semantically identical duplicates in hash maps and similar use cases.
	 */
	@SuppressWarnings("unchecked")
	static <V extends Vector3D> V withoutNegativeZero(V v) {
		if (v instanceof VectorXYZ vXYZ) {
			return (V) new VectorXYZ(withoutNegativeZero(vXYZ.x), withoutNegativeZero(vXYZ.y), withoutNegativeZero(vXYZ.z));
		} else {
			VectorXZ vXZ = (VectorXZ)v;
			return (V) new VectorXZ(withoutNegativeZero(vXZ.x), withoutNegativeZero(vXZ.z));
		}
	}

	static double withoutNegativeZero(double value) {
		return Double.valueOf(-0.0).equals(value) ? 0.0 : value;
	}

}
