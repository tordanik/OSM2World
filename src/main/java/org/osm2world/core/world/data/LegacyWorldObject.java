package org.osm2world.core.world.data;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.MeshTarget;
import org.osm2world.core.target.common.mesh.LevelOfDetail;
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
		return meshTarget.getMeshes().stream()
				.map(mesh -> new Mesh(mesh.geometry, mesh.material, getLodRange().getLeft(), getLodRange().getRight()))
				.toList();
	}

	/** returns a pair of min and max LOD for this {@link LegacyWorldObject}'s implicit {@link Mesh}es */
	default Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
		return Pair.of(LevelOfDetail.LOD0, LevelOfDetail.LOD4);
	}

}
