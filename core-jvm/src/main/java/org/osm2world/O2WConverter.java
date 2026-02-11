package org.osm2world;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.conversion.ProgressListener;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_elevation.creation.SRTMData;
import org.osm2world.math.geo.GeoBounds;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.osm.creation.OSMDataReader;
import org.osm2world.output.Output;
import org.osm2world.scene.Scene;
import org.osm2world.util.image.ImageImplementationJvm;
import org.osm2world.util.image.ImageUtil;
import org.osm2world.util.json.JsonImplementationJvm;
import org.osm2world.util.json.JsonUtil;
import org.osm2world.util.uri.JvmHttpClient;
import org.osm2world.util.uri.LoadUriUtil;

/**
 * This is the main class for using OSM2World as a library.
 * It offers functionality to convert data in the OSM data format to 3D models.
 * OSM2World is typically used by first constructing an {@link O2WConverter} instance,
 * potentially setting a few options, and then using one of the methods to perform one or more conversions.
 */
public class O2WConverter {

	static {
		LoadUriUtil.setClientFactory(JvmHttpClient::new);
		JsonUtil.setImplementation(new JsonImplementationJvm());
		ImageUtil.setImplementation(new ImageImplementationJvm());
	}

	private O2WConfig config = new O2WConfig();
	private final List<ProgressListener> listeners = new ArrayList<>();

	/**
	 * sets an {@link O2WConfig} object with settings that controls various aspects of OSM2World.
	 * If this method isn't called, this {@link O2WConverter} will use default values.
	 */
	public void setConfig(O2WConfig config) {
		this.config = config;
	}

	/**
	 * registers a progress listener which will receive updates about the progress of a <code>convert</code> call
	 */
	public void addProgressListener(ProgressListener listener) {
		listeners.add(listener);
	}

	/**
	 * converts data from an {@link OSMDataReader} into a 3D scene
	 * and optionally writes it to one or more {@link Output}s.
	 *
	 * @param osmDataReader  input data source
	 * @param bounds         the geographic region to convert. This can be null if all data from the data source should
	 *                       be used, e.g. if the data source is a small local .osm file.
	 * @param mapProjection  projection for converting between {@link LatLon} and local coordinates in {@link MapData}.
	 *                       May be null, in which case a default map projection will be used.
	 * @param outputs        receivers of the conversion results
	 * @return               the scene which has been produced as the result of the conversion.
	 *                       This will already have been written to all outputs,
	 *                       so you can ignore it unless you want to process it yourself.
	 *                       Regardless of input values, this scene will have a non-null {@link MapProjection}.
	 */
	public Scene convert(OSMDataReader osmDataReader, @Nullable GeoBounds bounds, @Nullable MapProjection mapProjection,
			Output... outputs) throws IOException {
		return new O2WConverterImpl(config, listeners).convert(osmDataReader, bounds, mapProjection, outputs);
	}

	/**
	 * converts {@link MapData} into a 3D scene
	 * and optionally writes it to one or more {@link Output}s.
	 *
	 * @param mapData        input data. Usually converted from some input data source
	 *                       or created with {@link MapDataBuilder}.
	 * @param mapProjection  projection for converting between {@link LatLon} and local coordinates in {@link MapData}.
	 *                       May be null, but that prevents accessing additional data sources such as {@link SRTMData}.
	 * @param outputs        receivers of the conversion results
	 * @return               the scene which has been produced as the result of the conversion.
	 *                       This will already have been written to all outputs,
	 *                       so you can ignore it unless you want to process it yourself.
	 */
	public Scene convert(MapData mapData, @Nullable MapProjection mapProjection, Output... outputs) {
		return new O2WConverterImpl(config, listeners).convert(mapData, mapProjection, outputs);
	}

}
