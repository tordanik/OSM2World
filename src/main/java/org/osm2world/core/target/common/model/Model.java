package org.osm2world.core.target.common.model;

import java.util.List;

import org.osm2world.core.math.VectorXYZ;
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
	 * @param target     target for the model; != null
	 * @param direction  rotation of the model in the XZ plane, as an angle in radians
	 * @param height     height of the model; null for default (unspecified) height
	 * @param width      width of the model; null for default (unspecified) width
	 * @param length     length of the model; null for default (unspecified) length
	 */
	public default void render(Target target, VectorXYZ position,
			double direction, Double height, Double width, Double length) {
		this.render(target, new InstanceParameters(position, direction, height, width, length));
	}

	/** see {@link #render(Target, VectorXYZ, double, Double, Double, Double)} */
	public default void render(Target target, InstanceParameters params) {
		buildMeshes(params).forEach(mesh -> target.drawMesh(mesh));
	}

}