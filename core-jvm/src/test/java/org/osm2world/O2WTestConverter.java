package org.osm2world;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.conversion.ProgressListener;
import org.osm2world.map_data.data.MapData;
import org.osm2world.math.geo.GeoBounds;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.osm.creation.OSMDataReader;
import org.osm2world.output.Output;
import org.osm2world.scene.Scene;

/**
 * O2WConverter duplicate for unit tests.
 * Exists to avoid having to make {@link O2WConverterImpl} public.
 */
public class O2WTestConverter {

	private O2WConfig config = new O2WConfig();
	private final List<ProgressListener> listeners = new ArrayList<>();

	public void setConfig(O2WConfig config) {
		this.config = config;
	}

	public void addProgressListener(ProgressListener listener) {
		listeners.add(listener);
	}

	public Scene convert(OSMDataReader osmDataReader, @Nullable GeoBounds bounds, @Nullable MapProjection mapProjection,
			Output... outputs) throws IOException {
		return new O2WConverterImpl(config, listeners).convert(osmDataReader, bounds, mapProjection, outputs);
	}

	public Scene convert(MapData mapData, @Nullable MapProjection mapProjection, Output... outputs) throws IOException {
		return new O2WConverterImpl(config, listeners).convert(mapData, mapProjection, outputs);
	}

}
