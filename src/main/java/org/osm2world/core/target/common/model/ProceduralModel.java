package org.osm2world.core.target.common.model;

import java.util.List;

import org.osm2world.core.target.CommonTarget;
import org.osm2world.core.target.common.MeshTarget;
import org.osm2world.core.target.common.mesh.Mesh;

/**
 * a model which is generated by code during runtime,
 * rather than being loaded as a static asset.
 */
public interface ProceduralModel extends Model {

	@Override
	default List<Mesh> buildMeshes(InstanceParameters params) {
		MeshTarget target = new MeshTarget();
		this.render(target, params);
		return target.getMeshes();
	}

	/**
	 * draws an instance of the model to any {@link CommonTarget}
	 *
	 * @param target target for the model; != null
	 */
	void render(CommonTarget target, InstanceParameters params);

}
