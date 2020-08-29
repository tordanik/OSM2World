package org.osm2world.core.osm.creation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.imintel.mbtiles4j.MBTilesReadException;
import org.imintel.mbtiles4j.MBTilesReader;
import org.imintel.mbtiles4j.Tile;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.target.common.rendering.TileNumber;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.dataset.MapDataSetLoader;
import de.topobyte.osm4j.pbf.seq.PbfIterator;

/**
 * {@link OSMDataReader} fetching a single tile from a MBTiles sqlite database which contains .osm.pbf data.
 */
public class MbtilesReader implements OSMDataReader {

	private final File mbtilesFile;
	private final TileNumber tile;

	public MbtilesReader(File mbtilesFile, TileNumber tile) {
		this.mbtilesFile = mbtilesFile;
		this.tile = tile;
	}

	@Override
	public OSMData getData() throws IOException {

		MBTilesReader r = null;

		try {

			r = new MBTilesReader(mbtilesFile);

			// get the tile; note that mbtiles is using TMS tile coords, which have a flipped y axis
			Tile t = r.getTile(tile.zoom, tile.x, tile.flippedY());

			try (InputStream is = t.getData()) {

				OsmIterator iterator = new PbfIterator(is, true);

				InMemoryMapDataSet data = MapDataSetLoader.read(iterator, true, true, true);
				return new OSMData(data);

			}

		} catch (MBTilesReadException e) {
			throw new IOException(e);
		} finally {
			if (r != null) {
				r.close();
			}
		}

	}

}
