package org.osm2world.map_elevation.creation;

import java.net.URI;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.util.tiles.PMTilesTileSet;
import org.osm2world.util.tiles.TileUriPattern;
import org.osm2world.util.tiles.UriTileSet;

public final class TerrainEleDataUtil {

	private TerrainEleDataUtil() {}

	/**
	 * Constructs {@link TerrainEleDataSource} based on the config options provided by the user
	 */
	public static @Nullable TerrainEleDataSource eleDataSourceFromConfig(O2WConfig config) {

		if (config.srtmDir() != null) {
			return new SRTMDataSource(config.srtmDir());
		} else {
			Object url = config.eleDataUrl();
			if (url instanceof TileUriPattern pattern) {
				return new TerrariumXYZDataSource(new UriTileSet(pattern));
			} else if (url instanceof URI uri) {
				if (uri.getPath().endsWith(".pmtiles")) {
					return new TerrariumXYZDataSource(new PMTilesTileSet(uri));
				} else {
					// TODO support URIs pointing to the root directory of a Terrarium XYZ or SRTM HGT tileset
					return null;
				}
			} else {
				return null;
			}
		}

	}

}
