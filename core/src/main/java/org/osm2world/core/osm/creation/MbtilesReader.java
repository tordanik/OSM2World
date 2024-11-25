package org.osm2world.core.osm.creation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.imintel.mbtiles4j.MBTilesReadException;
import org.imintel.mbtiles4j.MBTilesReader;
import org.imintel.mbtiles4j.Tile;
import org.osm2world.core.map_data.creation.LatLonBounds;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.target.common.rendering.TileNumber;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.dataset.MapDataSetLoader;
import de.topobyte.osm4j.pbf.seq.PbfIterator;

/**
 * {@link OSMDataReader} fetching a single tile from a MBTiles sqlite database which contains .osm.pbf data.
 *
 * @param file  the MBTiles file this reader is obtaining data from
 */
public record MbtilesReader(File file) implements OSMDataReader {

	/**
	 * map of existing readers.
	 * Necessary to avoid more than one reader (and thus connection) to be created by separate threads.
	 * Access through {@link #getReader(File)}!
	 * //TODO move into a separate MbtilesReaderPool class so I can enforce access restrictions?
	 */
	private static final Map<File, MBTilesReader> readerMap = new HashMap<>();
	//TODO reference counter, closing it

	private static synchronized MBTilesReader getReader(File mbtilesFile)
			throws MBTilesReadException, FileNotFoundException {

		if (!readerMap.containsKey(mbtilesFile)) {
			if (!mbtilesFile.exists()) {
				throw new FileNotFoundException("MBTiles file does not exist: " + mbtilesFile);
			}
			readerMap.put(mbtilesFile, new MBTilesReader(mbtilesFile));
		}

		return readerMap.get(mbtilesFile);

	}

	@Override
	public OSMData getData(TileNumber tile) throws IOException {

		try {

			MBTilesReader r = getReader(file);

			// get the tile; note that mbtiles is using TMS tile coords, which have a flipped y-axis
			Tile t = r.getTile(tile.zoom, tile.x, tile.flippedY());

			try (InputStream is = t.getData()) {

				OsmIterator iterator = new PbfIterator(is, true);

				InMemoryMapDataSet data = MapDataSetLoader.read(iterator, true, true, true);
				return new OSMData(data);

			}

		} catch (MBTilesReadException e) {
			throw new IOException(e);
		}

	}

	@Override
	public OSMData getData(LatLonBounds bounds) throws IOException {
		throw new UnsupportedOperationException("MbtilesReader does not support accessing data for arbitrary bounds");
	}

}
