package org.osm2world.target.common.rendering;

import static org.osm2world.math.VectorXYZ.Y_UNIT;

import org.osm2world.math.VectorXYZ;

public record ImmutableCamera(VectorXYZ pos, VectorXYZ lookAt, VectorXYZ up) implements Camera {

	ImmutableCamera(VectorXYZ pos, VectorXYZ lookAt) {
		this(pos, lookAt, calculateUp(pos, lookAt));
	}

	private static VectorXYZ calculateUp(VectorXYZ pos, VectorXYZ lookAt) {
		VectorXYZ direction = lookAt.subtract(pos).normalize();
		VectorXYZ right = direction.crossNormalized(Y_UNIT);
		return right.crossNormalized(direction);
	}

}
