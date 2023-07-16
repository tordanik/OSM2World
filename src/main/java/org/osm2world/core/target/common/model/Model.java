package org.osm2world.core.target.common.model;

import java.util.List;

import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.mesh.Mesh;

/**
 * a single 3D model, defined in code or loaded from a file or other resource
 */
public interface Model {

	/**
	 * returns the meshes making up an instance of this {@link Model}.
	 */
	public List<Mesh> buildMeshes(InstanceParameters params);

	/**
	 * draws an instance of the model to any {@link Target}
	 *
	 * @param target target for the model; != null
	 */
	public default void render(Target target, InstanceParameters params) {
		buildMeshes(params).forEach(target::drawMesh);
	}

}