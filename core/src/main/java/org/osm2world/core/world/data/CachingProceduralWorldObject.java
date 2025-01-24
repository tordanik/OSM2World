package org.osm2world.core.world.data;

import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.core.target.common.mesh.LevelOfDetail;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.model.ModelInstance;

/**
 * subtype of {@link ProceduralWorldObject} which caches internal results to avoid repeated calculations
 */
abstract public class CachingProceduralWorldObject implements ProceduralWorldObject {

	private ProceduralWorldObject.Target target = null;
	private @Nullable LevelOfDetail lod;

	private void fillTargetIfNecessary() {
		if (target == null || (lod != null && getConfiguredLod() != null && lod != getConfiguredLod())) {
			lod = getConfiguredLod();
			target = new ProceduralWorldObject.Target();
			buildMeshesAndModels(target);
		}
	}

	@Override
	public List<Mesh> buildMeshes() {
		fillTargetIfNecessary();
		return target.meshes;
	}

	@Override
	public List<ModelInstance> getSubModels() {
		fillTargetIfNecessary();
		return target.subModels;
	}

	/**
	 * if results depend on LOD, returns the currently configured LOD.
	 * Can be null if this {@link ProceduralWorldObject} always produces geometry for all LOD.
	 */
	protected @Nullable LevelOfDetail getConfiguredLod() {
		return null;
	}

}
