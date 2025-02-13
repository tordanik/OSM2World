package org.osm2world;

import static java.util.Arrays.asList;

import java.io.IOException;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_elevation.creation.SRTMData;
import org.osm2world.math.geo.GeoBounds;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.osm.creation.OSMDataReader;
import org.osm2world.osm.data.OSMData;
import org.osm2world.output.Output;

/**
 * This is the main class for using OSM2World as a library.
 * It offers functionality to convert data in the OSM data format to 3D models.
 * OSM2World is typically used by first constructing an {@link O2WConverter} instance,
 * potentially setting a few options, and then using one of the methods to perform one or more conversions.
 */
public class O2WConverter {

	private O2WConfig config = new O2WConfig();

	/**
	 * sets an {@link O2WConfig} object with settings that controls various aspects of OSM2World.
	 * If this method isn't called, this {@link O2WConverter} will use default values.
	 */
	public void setConfig(O2WConfig config) {
		this.config = config;
	}

	/**
	 * converts data from an {@link OSMDataReader} into a 3D scene
	 *
	 * @param osmDataReader  input data source
	 * @param bounds         the geographic region to convert. This can be null if all data from the data source should
	 *                       be used, e.g. if the data source is a small local .osm file.
	 * @param mapProjection  projection for converting between {@link LatLon} and local coordinates in {@link MapData}.
	 *                       May be null, in which case a default map projection will be used.
	 * @param outputs        receivers of the conversion results
	 */
	public void convert(OSMDataReader osmDataReader, @Nullable GeoBounds bounds, @Nullable MapProjection mapProjection,
			Output... outputs) throws IOException {

		OSMData osmData = (bounds != null)
			? osmDataReader.getData(bounds.latLonBounds())
			: osmDataReader.getAllData();

		var cf = new ConversionFacade();
		cf.createRepresentations(osmData, null, config, asList(outputs));

	}

	/**
	 * converts {@link MapData} into a 3D scene.
	 *
	 * @param mapData        input data. Usually converted from some input data source
	 *                       or created with {@link MapDataBuilder}.
	 * @param mapProjection  projection for converting between {@link LatLon} and local coordinates in {@link MapData}.
	 *                       May be null, but that prevents accessing additional data sources such as {@link SRTMData}.
	 * @param outputs        receivers of the conversion results
	 */
	public void convert(MapData mapData, @Nullable MapProjection mapProjection, Output... outputs) throws IOException {

		var cf = new ConversionFacade();
		cf.createRepresentations(mapProjection, mapData, null, config, asList(outputs));

	}

}
