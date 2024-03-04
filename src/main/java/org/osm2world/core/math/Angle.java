package org.osm2world.core.math;

import static java.lang.Math.*;

/**
 * an angle, stored in radians.
 * Values are in range 0 (inclusive) to 2 PI (exclusive). Operations wrap around to stay in that range.
 */
public class Angle {

	public final double radians;

	private Angle(double radians) {
		if (radians < 0 || radians >= 2 * PI) throw new IllegalArgumentException("angle out of range: " + radians);
		this.radians = radians;
	}

	public double degrees() {
		return toDegrees(radians);
	}

	public Angle plus(Angle other) {
		return Angle.ofRadians(radians + other.radians);
	}

	public Angle minus(Angle other) {
		return Angle.ofRadians(radians - other.radians);
	}

	public Angle times(double factor) {
		return Angle.ofRadians(radians * factor);
	}

	public Angle div(double divisor) {
		return Angle.ofRadians(radians / divisor);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Angle
				&& ((Angle)obj).radians == this.radians;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(radians);
	}

	@Override
	public String toString() {
		return radians + " rad";
	}

	/**
	 * returns the angular difference in radians between two direction angles.
	 * The difference can be either in clockwise or counterclockwise direction,
	 * whichever is smaller.
	 *
	 * @return  value in range 0 to PI
	 */
	public static double radiansBetween(Angle a1, Angle a2) {
		return VectorXZ.angleBetween(VectorXZ.fromAngle(a1), VectorXZ.fromAngle(a2));
	}

	public static Angle ofRadians(double radians) {
		return new Angle(fitToRange(radians));
	}

	public static Angle ofDegrees(double degrees) {
		return ofRadians(toRadians(degrees));
	}

	private static double fitToRange(double radians) {
		double angle = radians % (2 * PI);
		return (angle + (2 * PI)) % (2 * PI);
	}

}
