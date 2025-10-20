package org.osm2world.scene.model;

import static org.osm2world.scene.mesh.LevelOfDetail.LOD0;
import static org.osm2world.scene.mesh.LevelOfDetail.LOD4;

import javax.annotation.Nullable;

import org.osm2world.math.VectorXYZ;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.mesh.LODRange;

/**
 * The parameters for each instance of a {@link Model}.
 *
 * @param position   position of the model's origin; != null
 * @param direction  rotation of the model in the XZ plane, as an angle in radians
 * @param height     height of the model; null for default (unspecified) height
 * @param color      color of the main part of the model; null for unaltered colors.
 *                   This only works with specially prepared models that have a material with FF00FF placeholder color
 * @param lodRange   LOD at which this instance is visible.
 */
public record InstanceParameters(VectorXYZ position, double direction, Double height,
								 @Nullable Color color, LODRange lodRange) {

	public InstanceParameters(VectorXYZ position, double direction, Double height) {
		this(position, direction, height, null, new LODRange(LOD0, LOD4));
	}

	public InstanceParameters(VectorXYZ position, double direction, @Nullable Color color, LODRange lodRange) {
		this(position, direction, null, color, lodRange);
	}

	public InstanceParameters(VectorXYZ position, double direction, LODRange lodRange) {
		this(position, direction, null, null, lodRange);
	}

	public InstanceParameters(VectorXYZ position, double direction) {
		this(position, direction, null, new LODRange(LOD0, LOD4));
	}

}
