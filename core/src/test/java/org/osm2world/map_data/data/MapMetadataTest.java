package org.osm2world.map_data.data;

import static org.junit.Assert.assertEquals;
import static org.osm2world.util.test.TestFileUtil.getTestFile;

import java.io.File;
import java.io.IOException;

import org.imintel.mbtiles4j.MBTilesReadException;
import org.junit.Test;
import org.osm2world.math.geo.TileNumber;

public class MapMetadataTest {

	@Test
	public void testMetadataFromFile() throws IOException {

		File jsonFile = getTestFile("metadata_only_locale.json");

		assertEquals(new MapMetadata("AT", null),
				MapMetadata.metadataFromJson(jsonFile));

	}

	@Test
	public void testMetadataForTile() throws MBTilesReadException, IOException {

		File tileMetadataDb = getTestFile("meta.mbtiles");

		assertEquals(new MapMetadata("DE", true),
				MapMetadata.metadataForTile(new TileNumber(13, 4401, 2827), tileMetadataDb));

	}

}
