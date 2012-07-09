package org.osm2world.core.math;

import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.List;

public class VectorXYZ implements Vector3D {

	public final double x, y, z;

	public VectorXYZ(double x2, double y2, double z2) {
		this.x = x2;
		this.y = y2;
		this.z = z2;
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	@Override
	public double getZ() {
		return z;
	}

	public double length() {
		return Math.sqrt(x*x + y*y + z*z);
	}

	public double lengthSquared() {
		return x*x + y*y + z*z;
	}

	public VectorXYZ normalize() {
		double length = length();
		return new VectorXYZ(x / length, y / length, z / length);
	}

	public VectorXYZ add(VectorXYZ other) {
		return new VectorXYZ(
				this.x + other.x,
				this.y + other.y,
				this.z + other.z);
	}
	
	public VectorXYZ add(VectorXZ other) {
		return new VectorXYZ(
				this.x + other.x,
				this.y,
				this.z + other.z);
	}

	public VectorXYZ add(double x, double y, double z) {
		return new VectorXYZ(
				this.x + x,
				this.y + y,
				this.z + z);
	}

	public VectorXYZ subtract(VectorXYZ other) {
		return new VectorXYZ(
				this.x - other.x,
				this.y - other.y,
				this.z - other.z);
	}


	public VectorXYZ subtract(VectorXZ other) {
		return new VectorXYZ(
				this.x - other.x,
				this.y,
				this.z - other.z);
	}

	public VectorXYZ cross(VectorXYZ other) {
		return new VectorXYZ(
				this.y * other.z - this.z * other.y,
				this.z * other.x - this.x * other.z,
				this.x * other.y - this.y * other.x);
	}

	/**
	 * same result as calling {@link #normalize()} after
	 * {@link #cross(VectorXYZ)}, but avoids creating a temporary vector
	 */
	public VectorXYZ crossNormalized(VectorXYZ other) {
		
		//cross
		double x = this.y * other.z - this.z * other.y;
		double y = this.z * other.x - this.x * other.z;
		double z = this.x * other.y - this.y * other.x;
		
		//normalize
		double length = sqrt(x*x + y*y + z*z);
		return new VectorXYZ(x / length, y / length, z / length);
		
	}

	public double dot(VectorXYZ other) {
		return this.x * other.x + this.y * other.y + this.z * other.z;
	}

	public VectorXYZ mult(double scalar) {
		return new VectorXYZ(x*scalar, y*scalar, z*scalar);
	}
	
	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}

	/**
	 * returns the result of rotating this vector around the x axis
	 * @param angleRad  angle in radians
	 */
	public VectorXYZ rotateX(double angleRad) {
		double sin = Math.sin(angleRad);
		double cos = Math.cos(angleRad);
		return new VectorXYZ(x, y*cos - z*sin, y*sin + z*cos);
	}

	/**
	 * returns the result of rotating this vector around the y axis
	 * @param angleRad  angle in radians
	 */
	public VectorXYZ rotateY(double angleRad) {
		double sin = Math.sin(angleRad);
		double cos = Math.cos(angleRad);
		return new VectorXYZ(sin*z + cos*x, y, cos*z - sin*x);
	}

	/**
	 * returns the result of rotating this vector around the z axis
	 * @param angleRad  angle in radians
	 */
	public VectorXYZ rotateZ(double angleRad) {
		double sin = Math.sin(angleRad);
		double cos = Math.cos(angleRad);
		return new VectorXYZ(x*cos - y*sin, x*sin + y*cos, z);
	}

	/**
	 * calculates the angle between this vector and other,
	 * but only if both are normalized!
	 */
	public double angleTo(VectorXYZ other) {
		return Math.acos(this.dot(other));
	}
	
	public double distanceTo(VectorXYZ other) {
		//SUGGEST (performance): don't create temporary vector
		return (other.subtract(this)).length();
	}
	
	public double distanceToSquared(VectorXYZ other) {
		//SUGGEST (performance): don't create temporary vector
		return (other.subtract(this)).lengthSquared();
	}
	
	public double distanceToXZ(VectorXZ other) {
		//SUGGEST (performance): don't create temporary vector
		return VectorXZ.distance(this.xz(), other);
	}
	
	public double distanceToXZ(VectorXYZ other) {
		//SUGGEST (performance): don't create temporary vector
		return VectorXZ.distance(this.xz(), other.xz());
	}
	
	public VectorXZ xz() {
		return new VectorXZ(x, z);
	}
	
	public VectorXYZ x(double x) {
		return new VectorXYZ(x, this.y, this.z);
	}
	
	public VectorXYZ y(double y) {
		return new VectorXYZ(this.x, y, this.z);
	}

	public VectorXYZ z(double z) {
		return new VectorXYZ(this.x, this.y, z);
	}

	public VectorXYZ invert() {
		return new VectorXYZ(-x, -y, -z);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof VectorXYZ)) {
			return false;
		}
		VectorXYZ other = (VectorXYZ) obj;
		return x == other.x && y == other.y && z == other.z;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(z);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	/**
	 * creates a VectorXYZ for any Vector3D object.
	 * If the Vector3D is already a VectorXYZ, this can return the original vector.
	 */
	public static VectorXYZ xyz(VectorXYZ vector3D) {
		if (vector3D instanceof VectorXYZ) {
			return (VectorXYZ)vector3D;
		} else {
			return new VectorXYZ(vector3D.getX(), vector3D.getY(), vector3D.getZ());
		}
	}
	
	public static List<VectorXYZ> addYList(List<VectorXYZ> list, double addY) {
		List<VectorXYZ> result = new ArrayList<VectorXYZ>(list.size());
		for (VectorXYZ listEntry : list) {
			result.add(listEntry.y(listEntry.y + addY));
		}
		return result;
	}
	
	public static final VectorXYZ NULL_VECTOR = new VectorXYZ(0, 0, 0);
	public static final VectorXYZ X_UNIT = new VectorXYZ(1, 0, 0);
	public static final VectorXYZ Y_UNIT = new VectorXYZ(0, 1, 0);
	public static final VectorXYZ Z_UNIT = new VectorXYZ(0, 0, 1);
	
}
