package org.osm2world.core.osm.creation;

import java.io.IOException;

import org.osm2world.core.osm.data.OSMData;

public interface OSMDataReader {
	public OSMData getData() throws IOException;
}
