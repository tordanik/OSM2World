package org.osm2world.math;

public interface Vector3D extends BoundedObject {

	public double getX();
	public double getY();
	public double getZ();

	public VectorXZ xz();

	private VectorXYZ xyz() {
		if (this instanceof VectorXYZ) {
			return (VectorXYZ) this;
		} else {
			return new VectorXYZ(getX(), getY(), getZ());
		}
	}

	public static double distance(Vector3D v1, Vector3D v2) {
		return v1.xyz().distanceTo(v2.xyz());
	}

}
