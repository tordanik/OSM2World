package org.osm2world.core.map_data.data;

import java.io.*;

import javax.annotation.Nullable;

import org.imintel.mbtiles4j.MBTilesReadException;
import org.imintel.mbtiles4j.MBTilesReader;
import org.osm2world.core.math.geo.TileNumber;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

/**
 * additional information associated with a {@link MapData} dataset that goes beyond what's directly part of the data.
 * For example, it includes information derived from country borders which contain the dataset, but are often far
 * beyond the dataset's bounding box.
 *
 * @param land  whether the dataset is at sea (false), on land (true), or unknown/mixed (null)
 */
public record MapMetadata (@Nullable String locale, @Nullable Boolean land) {

	public synchronized static MapMetadata metadataForTile(TileNumber tile, File tileMetadataDb)
			throws MBTilesReadException, IOException {

		var metadataReader = new MBTilesReader(tileMetadataDb);
		MapMetadata result = metadataForTile(tile, metadataReader);
		metadataReader.close();
		return result;

	}

	public synchronized static MapMetadata metadataForTile(TileNumber tile, MBTilesReader tileMetadataReader)
			throws MBTilesReadException, IOException {
		var metadataTile = tileMetadataReader.getTile(tile.zoom, tile.x, tile.flippedY());
		try (var jsonPayloadReader = new InputStreamReader(metadataTile.getData())) {
			return metadataFromJson(jsonPayloadReader);
		}
	}

	public static MapMetadata metadataFromJson(File metadataFile) throws IOException {
		try (var fileReader = new FileReader(metadataFile)) {
			return metadataFromJson(fileReader);
		}
	}

	private static MapMetadata metadataFromJson(Reader metadataReader) throws IOException {
		try (var jsonReader = new JsonReader(metadataReader)) {
			return new Gson().fromJson(jsonReader, MapMetadata.class);
		}
	}

}
