package org.osm2world.core.target.common.model;

import static org.osm2world.core.target.common.mesh.LevelOfDetail.LOD0;
import static org.osm2world.core.target.common.mesh.LevelOfDetail.LOD4;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.mesh.LODRange;

/**
 * The parameters for each instance of a {@link Model}.
 *
 * @param position   position of the model's origin; != null
 * @param direction  rotation of the model in the XZ plane, as an angle in radians
 * @param height     height of the model; null for default (unspecified) height
 * @param width      width of the model; null for default (unspecified) width
 * @param length     length of the model; null for default (unspecified) length
 * @param lodRange   LOD at which this instance is visible.
 */
public record InstanceParameters(VectorXYZ position, double direction, Double height, Double width, Double length, LODRange lodRange) {

	/** alternative constructor for an instance with no restrictions on LOD */
	public InstanceParameters(VectorXYZ position, double direction, Double height, Double width, Double length) {
		this(position, direction, height, width, length, new LODRange(LOD0, LOD4));
	}

	/** alternative constructor for an unscaled instance */
	public InstanceParameters(VectorXYZ position, double direction, LODRange lodRange) {
		this(position, direction, null, null, null, lodRange);
	}

	/** alternative constructor for an unscaled instance with no restrictions on LOD */
	public InstanceParameters(VectorXYZ position, double direction) {
		this(position, direction, null, null, null);
	}

}
