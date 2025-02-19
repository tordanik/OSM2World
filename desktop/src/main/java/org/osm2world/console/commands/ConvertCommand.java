package org.osm2world.console.commands;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import org.osm2world.console.commands.mixins.*;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.scene.mesh.LevelOfDetail;

import picocli.CommandLine;

@Command(name = "convert", description = "Convert OSM data to 3D models and output the result.")
public class ConvertCommand implements Callable<Integer> {

	@Option(names = {"--output", "-o"}, description = "output files")
	@Nullable File output = null;

	@Option(names = {"--tile"}, description = "defining the tile to convert", paramLabel = "zoom,x,y")
	TileNumber tile;

	@Option(names = {"--lod"}, description = "level of detail of the output", paramLabel="[01234]")
	@Nullable
	LevelOfDetail lod = null;

	@Option(names = {"--input_bbox"}, arity = "2..*",
			description="lat,lon pairs defining an input bounding box (does not work with all data sources)")
	List<LatLon> latLon;

	@Option(names = {"--input_query"}, description = "overpass query string")
	@Nullable String inputQuery = null;

	@CommandLine.ArgGroup()
	@Nullable CameraOptions cameraOptions = null;

	/* mixins */

	@CommandLine.Mixin
	private InputOptions inputOptions;

	@CommandLine.Mixin
	private ConfigOptions configOptions;

	@CommandLine.Mixin
	private LoggingOptions loggingOptions;

	@CommandLine.Mixin
	private MetadataOptions metadataOptions;


	@Override
	public Integer call() throws Exception {

		System.err.println("The convert subcommand has not been implemented yet." +
				" Please use the legacy command line interface for now.");

		return 1;

	}

}
