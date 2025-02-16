package org.osm2world.output.common;

import java.util.List;

import org.osm2world.output.DrawBasedOutput;
import org.osm2world.output.Output;
import org.osm2world.output.common.MeshStore.MeshMetadata;
import org.osm2world.output.common.mesh.Mesh;
import org.osm2world.world.data.WorldObject;

/**
 * An {@link Output} that collects everything that is being drawn as {@link Mesh}es.
 * {@link Mesh}es are in-memory representation of 3D geometry suitable for use with typical graphics APIs.
 */
public class MeshOutput extends AbstractOutput implements DrawBasedOutput {

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

	public List<MeshStore.MeshWithMetadata> getMeshesWithMetadata() {
		return meshStore.meshesWithMetadata();
	}

}