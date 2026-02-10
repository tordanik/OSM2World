package org.osm2world.map_data.creation;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.imintel.mbtiles4j.MBTilesReadException;
import org.imintel.mbtiles4j.MBTilesReader;
import org.osm2world.conversion.ConversionLog;
import org.osm2world.map_data.data.MapMetadata;
import org.osm2world.math.geo.TileNumber;

/**
 * Loads {@link org.osm2world.map_data.data.MapMetadata} from MBTiles files.
 */
public class MapMetadataMbtilesUtil {

	public synchronized static MapMetadata metadataForTile(TileNumber tile, File tileMetadataDb)
			throws MBTilesReadException, IOException {

		var metadataReader = new MBTilesReader(tileMetadataDb);
		MapMetadata result = metadataForTile(tile, metadataReader);
		metadataReader.close();
		return result;

	}

	public synchronized static MapMetadata metadataForTile(TileNumber tile, MBTilesReader tileMetadataReader)
			throws MBTilesReadException, IOException {
		return metadataForTile(tile, tileMetadataReader, false);
	}

	private static MapMetadata metadataForTile(TileNumber tile, MBTilesReader tileMetadataReader,
			boolean suppressErrors) throws MBTilesReadException, IOException {

		var metadataTile = tileMetadataReader.getTile(tile.zoom, tile.x, tile.flippedY());
		var metadataPayload = metadataTile != null ? metadataTile.getData() : null;

		if (metadataPayload != null) {
			try (var jsonPayloadReader = new InputStreamReader(metadataPayload)) {
				return MapMetadata.metadataFromJson(jsonPayloadReader);
			}
		} else {

			// Look for metadata of parent tiles
			TileNumber parent = tile.ancestor(tile.zoom - 1);
			MapMetadata result = metadataForTile(parent, tileMetadataReader, true);

			if (result == null && !suppressErrors) {
				ConversionLog.error("Could not read metadata for tile " + tile);
			}

			return result;

		}

	}

}
