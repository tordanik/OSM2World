package org.osm2world.core.osm.creation;

import java.io.IOException;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.creation.LatLonBounds;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.target.common.rendering.TileNumber;

/**
 * view of another {@link OSMDataReader} which may be limited to a bounding region.
 * In this case, {@link #getAllData()} will always return data for that region.
 */
public class OSMDataReaderView implements OSMDataReader {

	public final OSMDataReader reader;
	private final @Nullable LatLonBounds bounds;
	private final @Nullable TileNumber tileNumber;

	public OSMDataReaderView(OSMDataReader reader, LatLonBounds bounds) {
		this(reader, bounds, null);
	}

	public OSMDataReaderView(OSMFileReader reader) {
		this(reader, null, null);
	}

	public OSMDataReaderView(OSMDataReader reader, TileNumber tileNumber) {
		this(reader, null, tileNumber);
	}

	private OSMDataReaderView(OSMDataReader reader, @Nullable LatLonBounds bounds, @Nullable TileNumber tileNumber) {
		this.reader = reader;
		this.bounds = bounds;
		this.tileNumber = tileNumber;
		if ((tileNumber != null) && (bounds != null)) {
			throw new IllegalArgumentException("Can only use either tileNumber or bounds");
		}
	}

	@Override
	public OSMData getAllData() throws IOException {
		if (bounds != null) {
			return reader.getData(bounds);
		} else if (tileNumber != null) {
			return reader.getData(tileNumber);
		} else {
			return reader.getAllData();
		}
	}

}
