package org.osm2world.map_elevation.creation;

import static org.osm2world.map_elevation.creation.TerrariumXYZDataSource.DEFAULT_MAX_ZOOM;

import java.io.File;
import java.net.URI;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.util.tiles.PMTilesTileSet;
import org.osm2world.util.tiles.TileSet;
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
			return eleDataSourceFromUrlList(config.eleDataUrls());
		}

	}

	private static @Nullable TerrainEleDataSource eleDataSourceFromUrlList(List<?> urls) {

		return switch (urls.size()) {
			case 0 -> null;
			case 1 -> eleDataSourceFromUrl(urls.get(0), false);
			default -> new TerrainEleDataSourceWithFallback(
					eleDataSourceFromUrl(urls.get(0), true),
					eleDataSourceFromUrlList(urls.subList(1, urls.size()))
			);
		};

	}

	private static @Nullable TerrainEleDataSource eleDataSourceFromUrl(Object url, boolean hasFallback) {

		TileSet<byte[]> tileSet = null;

		if (url instanceof TileUriPattern pattern) {

			if (pattern.pattern().endsWith(".pmtiles")) {
				tileSet = new PMTilesTileSet(pattern);
			} else {
				tileSet = new UriTileSet(pattern);
			}

		} else if (url instanceof URI uri) {

			if (uri.getPath().endsWith(".pmtiles")) {
				tileSet = new PMTilesTileSet(uri);
			} else {

				if ("file".equals(uri.getScheme()) && new File(uri).isDirectory()) {
					File srtmDir = new File(uri);
					File[] contents = srtmDir.listFiles((f, name) -> name.toLowerCase().contains(".hgt"));
					if (contents != null && contents.length > 0) {
						return new SRTMDataSource(srtmDir);
					}
				}

				// TODO support URIs pointing to the root directory of a Terrarium XYZ or SRTM HGT tileset
				return null;
			}

		}

		if (tileSet != null) {
			return new TerrariumXYZDataSource(DEFAULT_MAX_ZOOM, tileSet, hasFallback);
		} else {
			return null;
		}

	}

}
