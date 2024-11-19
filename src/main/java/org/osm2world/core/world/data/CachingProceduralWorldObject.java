package org.osm2world.core.world.data;

import java.util.Collection;
import java.util.List;

import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.model.ModelInstance;
import org.osm2world.core.world.attachment.AttachmentSurface;

/**
 * subtype of {@link ProceduralWorldObject} which caches internal results to avoid repeated calculations
 */
abstract public class CachingProceduralWorldObject implements ProceduralWorldObject {

	private ProceduralWorldObject.Target target = null;

	private void fillTargetIfNecessary() {
		if (target == null) {
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

	@Override
	public Collection<AttachmentSurface> getAttachmentSurfaces() {
		fillTargetIfNecessary();
		return target.attachmentSurfaces;
	}

}
