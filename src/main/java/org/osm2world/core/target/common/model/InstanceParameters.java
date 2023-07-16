package org.osm2world.core.target.common.model;

import org.osm2world.core.math.VectorXYZ;

/**
 * The parameters for each instance of a {@link Model}.
 *
 * @param position   position of the model's origin; != null
 * @param direction  rotation of the model in the XZ plane, as an angle in radians
 * @param height     height of the model; null for default (unspecified) height
 * @param width      width of the model; null for default (unspecified) width
 * @param length     length of the model; null for default (unspecified) length
 */
public record InstanceParameters(VectorXYZ position, double direction, Double height, Double width, Double length) {

	/** alternative constructor for an unscaled instance */
	public InstanceParameters(VectorXYZ position, double direction) {
		this(position, direction, null, null, null);
	}

}
