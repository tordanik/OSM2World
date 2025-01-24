package org.osm2world.core.map_data.data;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.imintel.mbtiles4j.MBTilesReadException;
import org.junit.Test;
import org.osm2world.core.target.common.rendering.TileNumber;

public class MapMetadataTest {

	@Test
	public void testMetadataFromFile() throws IOException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File jsonFile = new File(classLoader.getResource("metadata_only_locale.json").getFile());

		assertEquals(new MapMetadata("AT", null),
				MapMetadata.metadataFromJson(jsonFile));

	}

	@Test
	public void testMetadataForTile() throws MBTilesReadException, IOException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File tileMetadataDb = new File(classLoader.getResource("meta.mbtiles").getFile());

		assertEquals(new MapMetadata("DE", true),
				MapMetadata.metadataForTile(new TileNumber(13, 4401, 2827), tileMetadataDb));

	}

}
