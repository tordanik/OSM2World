package org.osm2world.map_elevation.creation;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.util.tiles.TileUriPattern;

public final class TerrainElevationDataUtil {

	private TerrainElevationDataUtil() {}

	/**
	 * Constructs {@link TerrainElevationData} based on the config options provided by the user
	 */
	public static @Nullable TerrainElevationData eleDataSourceFromConfig(O2WConfig config) {

		if (config.srtmDir() != null) {
			return new SRTMData(config.srtmDir());
		} else {
			Object url = config.eleDataUrl();
			if (url instanceof TileUriPattern pattern) {
				return new TerrariumXYZData(pattern);
			} else {
				return null;
			}
		}

	}

}
