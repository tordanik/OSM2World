package org.osm2world.osm.creation;

import java.io.IOException;

import javax.annotation.Nullable;

import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.osm.data.OSMData;

/**
 * view of another {@link OSMDataReader} which may be limited to a bounding region.
 * In this case, {@link #getAllData()} will always return data for that region.
 */
public class OSMDataReaderView implements OSMDataReader {

	public final OSMDataReader reader;
	private final @Nullable LatLonBounds bounds;
	private final @Nullable TileNumber tileNumber;
	private final @Nullable String queryString;

	private @Nullable OSMData getAllDataCache = null;

	public OSMDataReaderView(OSMDataReader reader, LatLonBounds bounds) {
		this(reader, bounds, null);
	}

	public OSMDataReaderView(OSMDataReader reader) {
		this(reader, null, null);
	}

	public OSMDataReaderView(OSMDataReader reader, TileNumber tileNumber) {
		this(reader, null, tileNumber);
	}

	public OSMDataReaderView(OverpassReader reader, String queryString) {
		this.reader = reader;
		this.bounds = null;
		this.tileNumber = null;
		this.queryString = queryString;
	}

	private OSMDataReaderView(OSMDataReader reader, @Nullable LatLonBounds bounds, @Nullable TileNumber tileNumber) {
		this.reader = reader;
		this.bounds = bounds;
		this.tileNumber = tileNumber;
		this.queryString = null;
		if ((tileNumber != null) && (bounds != null)) {
			throw new IllegalArgumentException("Can only use either tileNumber or bounds");
		}
	}

	@Override
	public OSMData getAllData() throws IOException {
		if (this.getAllDataCache == null) {
			if (bounds != null) {
				this.getAllDataCache = reader.getData(bounds);
			} else if (tileNumber != null) {
				this.getAllDataCache = reader.getData(tileNumber);
			} else if (queryString != null && reader instanceof OverpassReader overpassReader) {
				this.getAllDataCache = overpassReader.getData(queryString);
			} else {
				this.getAllDataCache = reader.getAllData();
			}
		}
		return this.getAllDataCache;
	}

	public LatLonBounds getBounds() throws IOException {
		if (bounds != null) {
			return bounds;
		} else if (tileNumber != null) {
			return tileNumber.latLonBounds();
		} else {
			return getAllData().getLatLonBounds();
		}
	}

}
