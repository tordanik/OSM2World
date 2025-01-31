package org.osm2world.core.osm.creation;

import java.io.IOException;

import org.osm2world.core.math.geo.LatLonBounds;
import org.osm2world.core.math.geo.TileNumber;
import org.osm2world.core.osm.data.OSMData;

/**
 * a data source which provides access to map data using the OpenStreetMap data model
 */
public interface OSMDataReader {

	/**
	 * returns all available data from this data source.
	 * Will fail for data sources which provide access to entire databases or collections of tiles.
	 */
	public default OSMData getAllData() throws IOException {
		throw new UnsupportedOperationException("Getting all data is not supported for this data source" +
				" (" + this.getClass().getSimpleName() +")");
	}

	public default OSMData getData(TileNumber tile) throws IOException {
		return getData(tile.bounds());
	}

	public default OSMData getData(LatLonBounds bounds) throws IOException {
		return getAllData();
	}

}
