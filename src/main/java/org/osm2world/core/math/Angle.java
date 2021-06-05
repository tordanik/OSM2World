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

	public static Angle ofRadians(double radians) {
		return new Angle(fitToRange(radians));
	}

	public static Angle ofDegrees(double degrees) {
		return ofRadians(toRadians(degrees));
	}

	private static double fitToRange(double radians) {
		if (radians < 0) {
			return fitToRange(radians + 2 * PI);
		} else if (radians >= 2 * PI) {
			return fitToRange(radians - 2 * PI);
		} else {
			return radians;
		}
	}

}
