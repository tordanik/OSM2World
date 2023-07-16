package org.osm2world.core.target.common.model;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.Target;

/**
 * The parameters for each instance of a {@link Model}.
 * See {@link Model#render(Target, VectorXYZ, double, Double, Double, Double)}
 * for the parameters' definitions.
 */
public final class InstanceParameters {

	public final VectorXYZ position;
	public final double direction;
	public final Double height;
	public final Double width;
	public final Double length;

	public InstanceParameters(VectorXYZ position, double direction, Double height, Double width, Double length) {
		this.position = position;
		this.direction = direction;
		this.height = height;
		this.width = width;
		this.length = length;
	}

	/** alternative constructor for an unscaled instance */
	public InstanceParameters(VectorXYZ position, double direction) {
		this(position, direction, null, null, null);
	}

}
