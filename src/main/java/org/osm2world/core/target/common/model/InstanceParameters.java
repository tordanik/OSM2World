package org.osm2world.core.target.common.model;

import static org.osm2world.core.target.common.mesh.LevelOfDetail.LOD0;
import static org.osm2world.core.target.common.mesh.LevelOfDetail.LOD4;

import java.awt.*;

import javax.annotation.Nullable;

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
 * @param color      color of the main part of the model; null for unaltered colors.
 *                   This only works with specially prepared models that have a material with FF00FF placeholder color
 * @param lodRange   LOD at which this instance is visible.
 */
public record InstanceParameters(VectorXYZ position, double direction, Double height, Double width, Double length,
								 @Nullable Color color, LODRange lodRange) {

	public InstanceParameters(VectorXYZ position, double direction, Double height, Double width, Double length) {
		this(position, direction, height, width, length, null, new LODRange(LOD0, LOD4));
	}

	public InstanceParameters(VectorXYZ position, double direction, @Nullable Color color, LODRange lodRange) {
		this(position, direction, null, null, null, color, lodRange);
	}

	public InstanceParameters(VectorXYZ position, double direction, LODRange lodRange) {
		this(position, direction, null, null, null, null, lodRange);
	}

	public InstanceParameters(VectorXYZ position, double direction) {
		this(position, direction, null, null, null);
	}

}
