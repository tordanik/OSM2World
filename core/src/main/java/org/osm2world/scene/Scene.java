package org.osm2world.scene;

import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapData;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.output.common.MeshOutput;
import org.osm2world.output.common.mesh.Mesh;
import org.osm2world.world.data.WorldObject;

import com.google.common.collect.Iterables;

/**
 * A 3D scene created from map data.
 * Interim result of an OSM2World run (before it is written to any Output).
 */
public final class Scene {

	private final @Nullable MapProjection mapProjection;
	private final MapData mapData;

	/** caches the result of {@link #getMeshes()} */
	private List<Mesh> meshes = null;

	public Scene(@Nullable MapProjection mapProjection, MapData mapData) {
		this.mapProjection = mapProjection;
		this.mapData = mapData;
	}

	/**
	 * the map projection used to convert between geographic coordinates and the scene's coordinate system
	 */
	public @Nullable MapProjection getMapProjection() {
		return mapProjection;
	}

	/**
	 * the scene bounds. Some geometry in the scene may extend beyond those bounds.
	 */
	public AxisAlignedRectangleXZ getBoundary() {
		return mapData.getBoundary();
	}

	/**
	 * returns the underlying {@link MapData}
	 */
	public MapData getMapData() {
		return mapData;
	}

	/**
	 * returns all {@link WorldObject}s in this scene
	 */
	public Iterable<WorldObject> getWorldObjects() {
		return mapData.getWorldObjects();
	}

	/**
	 * returns all {@link WorldObject}s in this scene which are instances of a certain type.
	 */
	public <T> Iterable<T> getWorldObjects(Class<T> type) {
		return Iterables.filter(getWorldObjects(), type);
	}

	/**
	 * returns the {@link Mesh}es of all world objects
	 */
	public List<Mesh> getMeshes() {
		if (this.meshes == null) {
			var output = new MeshOutput();
			output.outputScene(this);
			this.meshes = output.getMeshes();
		}
		return this.meshes;
	}

}
