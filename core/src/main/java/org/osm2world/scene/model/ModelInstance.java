package org.osm2world.scene.model;

import java.util.List;

import org.osm2world.output.CommonTarget;
import org.osm2world.scene.mesh.Mesh;

/**
 * one instance of a {@link Model}
 */
public record ModelInstance(Model model, InstanceParameters params) {

	/**
	 * returns the model's meshes, with transformations based on {@link #params} already applied to them.
	 */
	public List<Mesh> getMeshes() {
		return model.buildMeshes(params);
	}

	/**
	 * draws the result of {@link #getMeshes()} to any target
	 */
	public void render(CommonTarget target) {
		getMeshes().forEach(target::drawMesh);
	}

}
