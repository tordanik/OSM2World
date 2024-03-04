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

	/**
	 * map of existing readers.
	 * Necessary to avoid more than one reader (and thus connection) to be created by separate threads.
	 * Access through {@link #getReader(File)}!
	 * //TODO move into a separate MbtilesReaderPool class so I can enforce access restrictions?
	 */
	private static Map<File, MBTilesReader> readerMap = new HashMap<>();
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

	private final File mbtilesFile;
	private final TileNumber tile;

	public MbtilesReader(File mbtilesFile, TileNumber tile) {
		this.mbtilesFile = mbtilesFile;
		this.tile = tile;
	}

	/** returns the MBTiles file this reader is obtaining data from */
	public File getFile() {
		return mbtilesFile;
	}

	@Override
	public OSMData getData() throws IOException {

		MBTilesReader r = null;

		try {

			r = getReader(mbtilesFile);

			// get the tile; note that mbtiles is using TMS tile coords, which have a flipped y axis
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

}
