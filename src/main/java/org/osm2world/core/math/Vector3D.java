package org.osm2world.core.math;

/**
 * Pull-up method Refactoring Done
 * Method length() pull up from the child VectorXZ and VectorXYZ;
 * @return
 */
public interface Vector3D extends BoundedObject {

	public double getX();
	public double getY();
	public double getZ();

	public VectorXZ xz();
	public double length();

}
