package org.osm2world.core.target.common.model;

import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.MeshTarget;
import org.osm2world.core.target.common.mesh.Mesh;

/**
 * a {@link Model} that still uses "draw" methods of {@link Target}
 * instead of the new {@link #buildMeshes(InstanceParameters)} method.
 * This exists to smooth the transition.
 */
public interface LegacyModel extends Model {

	@Override
	public default List<Mesh> buildMeshes(InstanceParameters params) {
		MeshTarget target = new MeshTarget();
		this.render(target, params);
		return target.getMeshes();
	}

	@Override
	public abstract void render(Target target, VectorXYZ position,
			double direction, Double height, Double width, Double length);

	@Override
	public default void render(Target target, InstanceParameters params) {
		this.render(target, params.position, params.direction, params.height, params.width, params.length);
	}

}
