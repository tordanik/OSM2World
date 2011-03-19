package org.osm2world.core.math;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * immutable two-dimensional vector with x and z component
 */
public class VectorXZ implements Vector3D {

	public final double x;
	public final double z;
	
	@Override
	public double getX() {
		return x;
	}
	
	@Override
	public double getY() {
		return 0;
	}
	
	@Override
	public double getZ() {
		return z;
	}
		
	public VectorXZ(double x, double z) {
		this.x = x;
		this.z = z;
	}

	public double length() {
		return Math.sqrt(x*x + z*z);
	}

	public double lengthSquared() {
		return x*x + z*z;
	}
	
	public VectorXZ normalize() {
		double length = length();
		return new VectorXZ(x / length, z / length);
	}

	/**
	 * adds the parameter to this vector and returns the result
	 */
	public VectorXZ add(VectorXZ other) {
		return new VectorXZ(x + other.x, z + other.z);
	}
	
	/**
	 * subtracts the parameter from this vector and returns the result
	 */
	public VectorXZ subtract(VectorXZ other) {
		return new VectorXZ(x - other.x, z - other.z);
	}

	public VectorXZ mult(double scalar) {
		return new VectorXZ(x*scalar, z*scalar);
	}

	public VectorXZ invert() {
		return new VectorXZ(-x, -z);
	}

	public double dot(VectorXZ other) {
		return this.x * other.x + this.z * other.z;
	}
	
	/**
	 * returns the vector that would result from calculating the
	 * cross product of this vector (normalized and extended
	 * to three dimensions) and (0,1,0).
	 * 
	 * It's the vector that, seen from "above", points to the right
	 * side of this vector and is orthogonal to it.
	 * 
	 * The resulting vector's length is 1.
	 */
	public VectorXZ rightNormal() {
		double length = length();
		return new VectorXZ(z / length, -(x / length));
	}
	
	public double distanceTo(VectorXZ other) {
		return distance(this, other);
	}
	
	@Override
	public String toString() {
		return "(" + x + "," + z + ")";
	}
	
	public static final VectorXZ NULL_VECTOR = new VectorXZ(0, 0);	
	public static final VectorXZ X_UNIT = new VectorXZ(1, 0);	
	public static final VectorXZ Z_UNIT = new VectorXZ(0, 1);


	public VectorXYZ xyz(double y) {
		return new VectorXYZ(x, y, z);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof VectorXZ)) {
			return false;
		}
		VectorXZ other = (VectorXZ) obj;
		return x == other.x && z == other.z;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(z);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	
	//TODO: angle bisection as method in this class
	
	public static final double distance(VectorXZ v1, VectorXZ v2) {
		//SUGGEST (performance): don't create temporary vector
		return (v2.subtract(v1)).length();
	}
	
	public static final double distanceSquared(VectorXZ v1, VectorXZ v2) {
		//SUGGEST (performance): don't create temporary vector
		return (v2.subtract(v1)).lengthSquared();
	}
	
	public static final List<VectorXYZ> listXYZ(List<VectorXZ> vs, final double y) {
		return Lists.transform(vs, VectorXZ.xyzFunction(y));
	}
	

	public static final Function<VectorXZ, Double> distanceFunction(final VectorXZ v1) {
		
		return new Function<VectorXZ, Double>() {			
			@Override
			public Double apply(VectorXZ v2) {
				return distance(v1, v2);
			}
		};
		
	}
	
	public static final Function<VectorXZ, VectorXYZ> xyzFunction(final double y) {
		return new Function<VectorXZ, VectorXYZ>() {
			@Override
			public VectorXYZ apply(VectorXZ v) {
				return v.xyz(y);
			}
		};
	}
	
}
