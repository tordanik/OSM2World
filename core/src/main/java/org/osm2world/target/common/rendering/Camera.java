package org.osm2world.target.common.rendering;

import org.osm2world.math.VectorXYZ;

/**
 * Describes the position and direction from which a view onto the virtual world is rendered.
 * For orthographic rendering, you can use {@link OrthographicUtil} to construct a suitable camera.
 */
public interface Camera {

	VectorXYZ pos();

	VectorXYZ lookAt();

	VectorXYZ up();

	/** returns the view direction vector with length 1 */
	default VectorXYZ getViewDirection() {
		return lookAt().subtract(pos()).normalize();
	}

	/**
	 * returns the vector that is orthogonal to the connection
	 * between pos and lookAt and points to the right of it.
	 * The result has length 1.
	 */
	default VectorXYZ getRight() {
		return getViewDirection().crossNormalized(up());
	}

}
