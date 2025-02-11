package org.osm2world.target.common;

import java.util.List;

import org.osm2world.target.Target;
import org.osm2world.target.common.MeshStore.MeshMetadata;
import org.osm2world.target.common.mesh.Mesh;
import org.osm2world.world.data.WorldObject;

/**
 * a {@link Target} that collects everything that is being drawn as {@link Mesh}es.
 * {@link Mesh}es are in-memory representation of 3D geometry suitable for use with typical graphics APIs.
 */
public class MeshTarget extends AbstractTarget {

	protected final MeshStore meshStore = new MeshStore();

	protected WorldObject currentWorldObject = null;

	@Override
	public void beginObject(WorldObject object) {
		this.currentWorldObject = object;
	}

	@Override
	public void drawMesh(Mesh mesh) {

		MeshMetadata metadata = (currentWorldObject != null)
				? new MeshMetadata(currentWorldObject.getPrimaryMapElement().getElementWithId(),
						currentWorldObject.getClass())
				: new MeshMetadata(null, null);

		meshStore.addMesh(mesh, metadata);

	}

	public List<Mesh> getMeshes() {
		return meshStore.meshes();
	}

}