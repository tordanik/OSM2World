package org.osm2world.console.commands.mixins;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import org.imintel.mbtiles4j.MBTilesReadException;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.MapMetadata;
import org.osm2world.math.geo.TileNumber;

import picocli.CommandLine;

public class MetadataOptions {

	@CommandLine.Option(names = {"--metadataFile"}, description = "path to a JSON file with metadata, " +
			"or an mbtiles file with such JSON data for multiple tiles", paramLabel = "<path>")
	public @Nullable File metadataFile;

	public static O2WConfig addMetadataToConfig(@Nullable File metadataFile, @Nullable TileNumber tile,
			O2WConfig config) throws IOException {

		if (metadataFile != null) {

			MapMetadata metadata = null;

			if (metadataFile.getName().endsWith(".mbtiles")) {
				if (tile != null) {
					try {
						metadata = MapMetadata.metadataForTile(tile, metadataFile);
					} catch (MBTilesReadException e) {
						System.err.println("Cannot read tile metadata: " + e);
					}
				}
			} else {
				metadata = MapMetadata.metadataFromJson(metadataFile);
			}

			if (metadata != null && metadata.land() == Boolean.FALSE) {
				config = config.withProperty("isAtSea", true);
			}

		}

		return config;

	}

}
