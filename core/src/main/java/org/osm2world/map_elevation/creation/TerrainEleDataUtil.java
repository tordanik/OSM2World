package org.osm2world.map_elevation.creation;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.util.tiles.TileUriPattern;

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
				return new TerrariumXYZDataSource(pattern);
			} else {
				return null;
			}
		}

	}

}
