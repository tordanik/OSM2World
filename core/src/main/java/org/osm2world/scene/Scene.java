package org.osm2world.scene;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.MapData;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.output.common.MeshOutput;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.mesh.MeshStore;
import org.osm2world.world.data.WorldObject;

import com.google.common.collect.Iterables;

/**
 * A 3D scene created from map data.
 * Interim result of an OSM2World run (before it is written to any Output).
 */
public final class Scene {

	private final @Nullable MapProjection mapProjection;
	private final MapData mapData;

	/** caches the result of {@link #getMeshes()} and {@link #getMeshesWithMetadata()} */
	private final Map<O2WConfig, MeshStore> meshStoreCache = new HashMap<>();

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

	/** @see #getMeshes(O2WConfig) */
	public List<Mesh> getMeshes() {
		return getMeshes(null);
	}
	
	/**
	 * returns the {@link Mesh}es of all world objects
	 * 
	 * @param config  optional configuration object. Some preferences such as {@link O2WConfig#renderUnderground()}
	 *                will affect the result.
	 */
	public List<Mesh> getMeshes(@Nullable O2WConfig config) {
		var meshStore = loadMeshStore(config);
		return meshStore.meshes();
	}

	/** @see #getMeshesWithMetadata(O2WConfig) */
	public List<MeshStore.MeshWithMetadata> getMeshesWithMetadata() {
		return getMeshesWithMetadata(null);
	}

	/**
	 * returns the same meshes as {@link #getMeshes(O2WConfig)}, but includes metadata
	 */
	public List<MeshStore.MeshWithMetadata> getMeshesWithMetadata(@Nullable O2WConfig config) {
		var meshStore = loadMeshStore(config);
		return meshStore.meshesWithMetadata();
	}

	private MeshStore loadMeshStore(@Nullable O2WConfig config) {
		config = Objects.requireNonNullElse(config, new O2WConfig());
		if (!this.meshStoreCache.containsKey(config)) {
			var output = new MeshOutput();
			output.setConfiguration(config);
			output.outputScene(this);
			var meshStore = new MeshStore(output.getMeshesWithMetadata());
			this.meshStoreCache.put(config, meshStore);
		}
		return this.meshStoreCache.get(config);
	}

}
