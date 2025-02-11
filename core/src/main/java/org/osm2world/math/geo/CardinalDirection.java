package org.osm2world.math.geo;

import static java.lang.Math.PI;

import org.osm2world.math.VectorXZ;

/**
 * one of the four cardinal directions
 */
public enum CardinalDirection {

	N, E, S, W;

	/**
	 * returns the closest cardinal direction for an angle
	 *
	 * @param angle angle to north direction in radians;
	 *              consistent with {@link VectorXZ#angle()}
	 */
	public static CardinalDirection closestCardinal(double angle) {
		angle = angle % (2 * PI);
		if (angle < PI / 4) {
			return N;
		} else if (angle < 3 * PI / 4) {
			return E;
		} else if (angle < 5 * PI / 4) {
			return S;
		} else if (angle < 7 * PI / 4) {
			return W;
		} else {
			return N;
		}
	}

	public boolean isOppositeOf(CardinalDirection other) {
		return this == N && other == S
				|| this == E && other == W
				|| this == S && other == N
				|| this == W && other == E;
	}

}
