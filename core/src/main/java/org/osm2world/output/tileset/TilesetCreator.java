package org.osm2world.output.tileset;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.output.common.compression.Compression;
import org.osm2world.output.gltf.GltfFlavor;
import org.osm2world.output.gltf.GltfOutput;

/**
 * creates Cesium 3D Tiles. Uses {@link GltfOutput} to generate the tile content.
 */
public class TilesetCreator {

	final O2WConfig config;
	final GltfFlavor gltfFlavor;
	final Compression gltfCompression;
	final org.osm2world.math.geo.MapProjection mapProjection;

	public TilesetCreator(O2WConfig config, GltfFlavor gltfFlavor, Compression gltfCompression,
			MapProjection mapProjection) {
		this.config = config;
		this.gltfFlavor = gltfFlavor;
		this.gltfCompression = gltfCompression;
		this.mapProjection = mapProjection;
	}

}
