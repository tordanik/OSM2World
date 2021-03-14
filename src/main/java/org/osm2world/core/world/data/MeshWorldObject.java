package org.osm2world.core.world.data;

import java.util.List;

import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.mesh.Mesh;

/**
 * a {@link WorldObject} that already uses the new {@link #buildMeshes()} mechanism
 * instead of the old {@link WorldObject#renderTo(org.osm2world.core.target.Target)} method.
 * This exists to smooth the transition.
 */
public interface MeshWorldObject extends WorldObject {

	@Override
	default void renderTo(Target target) {
		for (Mesh mesh : buildMeshes()) {
			// TODO implement
		}
	}

	/**
	 * returns the meshes making up this {@link WorldObject}.
	 */
	public List<Mesh> buildMeshes();

}
