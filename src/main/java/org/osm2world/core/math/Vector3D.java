package org.osm2world.core.math;

import org.osm2world.core.math.datastructures.IntersectionTestObject;

public interface Vector3D extends IntersectionTestObject {

	public double getX();
	public double getY();
	public double getZ();

	public VectorXZ xz();

}
