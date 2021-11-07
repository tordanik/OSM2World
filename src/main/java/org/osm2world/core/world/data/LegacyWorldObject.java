package org.osm2world.core.world.data;

import java.util.List;

import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.MeshTarget;
import org.osm2world.core.target.common.mesh.Mesh;

/**
 * a {@link WorldObject} that still uses "draw" methods of {@link Target}
 * instead of the new {@link #buildMeshes()} method.
 * This exists to smooth the transition.
 */
public interface LegacyWorldObject extends WorldObject, Renderable {

	@Override
	default List<Mesh> buildMeshes() {
		MeshTarget meshTarget = new MeshTarget();
		renderTo(meshTarget);
		return meshTarget.getMeshes();
	}

}
