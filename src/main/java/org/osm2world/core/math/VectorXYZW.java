package org.osm2world.core.math;

public class VectorXYZW implements Vector4D {

	public final double x, y, z, w;

	public VectorXYZW(double x2, double y2, double z2, double w2) {
		this.x = x2;
		this.y = y2;
		this.z = z2;
		this.w = w2;
	}

	public VectorXYZW(VectorXYZ v, double w2) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
		this.w = w2;
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

	@Override
	public double getW() {
		return z;
	}

	public double length() {
		return Math.sqrt(x*x + y*y + z*z + w*w);
	}

	public double lengthSquared() {
		return x*x + y*y + z*z + w*w;
	}

	public VectorXYZW normalize() {
		double length = length();
		return new VectorXYZW(x / length, y / length, z / length, w / length);
	}

	public VectorXYZW add(VectorXYZW other) {
		return new VectorXYZW(
				this.x + other.x,
				this.y + other.y,
				this.z + other.z,
				this.w + other.w);
	}

	public VectorXYZW add(double x, double y, double z, double w) {
		return new VectorXYZW(
				this.x + x,
				this.y + y,
				this.z + z,
				this.w + w);
	}

	public VectorXYZW subtract(VectorXYZW other) {
		return new VectorXYZW(
				this.x - other.x,
				this.y - other.y,
				this.z - other.z,
				this.w - other.w);
	}

	public VectorXYZW mult(double scalar) {
		return new VectorXYZW(x*scalar, y*scalar, z*scalar, w*scalar);
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ", " + w + ")";
	}

	public double distanceTo(VectorXYZW other) {
		//SUGGEST (performance): don't create temporary vector
		return (other.subtract(this)).length();
	}

	public double distanceToSquared(VectorXYZW other) {
		//SUGGEST (performance): don't create temporary vector
		return (other.subtract(this)).lengthSquared();
	}

	public VectorXYZW x(double x) {
		return new VectorXYZW(x, this.y, this.z, this.w);
	}

	public VectorXYZW y(double y) {
		return new VectorXYZW(this.x, y, this.z, this.w);
	}

	public VectorXYZW z(double z) {
		return new VectorXYZW(this.x, this.y, z, this.w);
	}

	public VectorXYZW invert() {
		return new VectorXYZW(-x, -y, -z, -w);
	}

	public VectorXYZ xyz() {
		return new VectorXYZ(x, y, z);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof VectorXYZW)) {
			return false;
		}
		VectorXYZW other = (VectorXYZW) obj;
		return x == other.x && y == other.y && z == other.z && w == other.w;
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
		temp = Double.doubleToLongBits(w);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	/**
	 * creates a VectorXYZ for any Vector4D object.
	 * If the Vector4D is already a VectorXYZ, this can return the original vector.
	 */
	public static VectorXYZW xyz(Vector4D vector4D) {
		if (vector4D instanceof VectorXYZW) {
			return (VectorXYZW)vector4D;
		} else {
			return new VectorXYZW(vector4D.getX(), vector4D.getY(), vector4D.getZ(), vector4D.getW());
		}
	}

	public static final VectorXYZW NULL_VECTOR = new VectorXYZW(0, 0, 0, 0);
	public static final VectorXYZW X_UNIT = new VectorXYZW(1, 0, 0, 0);
	public static final VectorXYZW Y_UNIT = new VectorXYZW(0, 1, 0, 0);
	public static final VectorXYZW Z_UNIT = new VectorXYZW(0, 0, 1, 0);
	public static final VectorXYZW W_UNIT = new VectorXYZW(0, 0, 0, 1);

}
