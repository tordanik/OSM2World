package org.osm2world.output.common;

import static java.util.Objects.requireNonNullElse;
import static org.osm2world.util.FaultTolerantIterationUtil.DEFAULT_EXCEPTION_HANDLER;
import static org.osm2world.util.FaultTolerantIterationUtil.forEach;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.output.CommonTarget;
import org.osm2world.output.Output;
import org.osm2world.scene.Scene;
import org.osm2world.scene.mesh.LevelOfDetail;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.mesh.MeshStore;
import org.osm2world.scene.mesh.TriangleGeometry;
import org.osm2world.world.data.WorldObject;

/**
 * output which internally translates the rendered scene into several draw* calls.
 * This is mostly a solution for transitioning from old-style OSM2World outputs
 * to the new solution based on {@link #outputScene}.
 */
public interface DrawBasedOutput extends Output, CommonTarget {

	default LevelOfDetail getLod() {
		return getConfiguration().getLod();
	}

	@Override
	default void outputScene(Scene scene) {
		this.outputScene(scene, false);
	}

	/**
	 * writes an entire {@link Scene} to this output.
	 * @param keepOpen  false to call {@link #finish()} at the end,
	 *                  true to allow further content to be written to this target.
	 */
	default void outputScene(Scene scene, boolean keepOpen) {

		forEach(scene.getWorldObjects(), (WorldObject r) -> {
			if (r.getParent() == null) {
				if (requireNonNullElse(getConfiguration(), new O2WConfig()).renderUnderground() || r.getGroundState() != GroundState.BELOW) {
					renderObject(r);
				}
			}
		}, (e, r) -> DEFAULT_EXCEPTION_HANDLER.accept(e, r.getPrimaryMapElement()));

		if (!keepOpen) {
			finish();
		}

	}

	/**
	 * renders one {@link WorldObject} to this output.
	 * Also sends {@link #beginObject(WorldObject)} calls.
	 */
	private void renderObject(WorldObject object) {
		beginObject(object);
		object.buildMeshes().forEach(this::drawMesh);
		object.getSubModels().forEach(it -> it.render(this));
	}

	/**
	 * announces the beginning of the draw* calls for a {@link WorldObject}.
	 * This makes it possible for implementations to group them, if desired.
	 * Otherwise, this can be ignored.
	 *
	 * @param object  the object that all draw method calls until the next beginObject belong to; can be null
	 */
	default void beginObject(@Nullable WorldObject object) {}

	default void drawMesh(Mesh mesh) {
		if (mesh.lodRange.contains(getLod())) {
			var converter = new MeshStore.ConvertToTriangles(getLod());
			TriangleGeometry tg = converter.applyToGeometry(mesh.geometry);
			drawTriangles(mesh.material, tg.triangles, tg.normalData.normals(), tg.texCoords);
		}
	}

	/**
	 * gives this output the chance to perform finish/cleanup operations
	 * after all objects have been drawn.
	 */
	default void finish() {};

}
