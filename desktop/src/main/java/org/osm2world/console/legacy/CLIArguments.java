package org.osm2world.console.legacy;

import java.io.File;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.console.commands.mixins.InputOptions.InputMode;
import org.osm2world.math.geo.CardinalDirection;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonEle;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.osm.creation.OverpassReader;
import org.osm2world.util.Resolution;

import com.lexicalscope.jewel.cli.Option;

interface CLIArguments {

	String OUTPUT_PATTERN = "(.*)\\.(?:obj|gltf(?:.gz)?|glb(?:.gz)?|pov|o2w.pbf(?:.gz)?|png|ppm|gd)";

	/* input and output files */

	@Option(description="the .osm input file", shortName="i")
	File getInput();
	boolean isInput();

	@Option(description="output files", shortName="o", pattern=OUTPUT_PATTERN)
	List<File> getOutput();
	boolean isOutput();

	@Option(description="properties file(s) with configuration parameters", defaultValue = "{}")
	List<File> getConfig();

	@Option(description="output size in pixels", pattern=Resolution.PATTERN)
	Resolution getResolution();
	boolean isResolution();

	@Option(description="level of detail of the output", pattern="[01234]", defaultToNull=true)
	@Nullable Integer getLod();

	/* other input options */

	@Option(description="input mode", longName="input_mode", defaultValue="FILE", pattern="FILE|OVERPASS")
	InputMode getInputMode();
	boolean isInputMode();

	@Option(description="lat,lon pairs defining an input bounding box (does not work with files)",
			longName="input_bbox", pattern=LatLon.PATTERN)
	List<LatLon> getInputBoundingBox();
	boolean isInputBoundingBox();

	@Option(description="zoom,x,y defining an input tile (used for mbtiles, Geodesk and Overpass)",
			pattern=TileNumber.PATTERN)
	TileNumber getTile();
	boolean isTile();

	@Option(description="overpass query string", longName="input_query")
	String getInputQuery();
	boolean isInputQuery();

	@Option(description="overpass instance to use", longName="overpass_url", defaultValue=OverpassReader.DEFAULT_API_URL)
	String getOverpassURL();
	boolean isOverpassURL();

	/* camera */

	@Option(description="downwards angle of orthographic view in degrees",
			longName="oview.angle", defaultValue="30")
	double getOviewAngle();
	boolean isOviewAngle();

	@Option(description="direction from which the orthographic view is rendered",
			pattern="[NESW]", longName="oview.from", defaultValue="S")
	CardinalDirection getOviewFrom();
	boolean isOviewFrom();

	@Option(description="lat,lon pairs defining a bounding box for orthographic view",
			pattern=LatLon.PATTERN, longName="oview.bbox")
	List<LatLon> getOviewBoundingBox();
	boolean isOviewBoundingBox();

	@Option(description="zoom,x,y triples of tiles defining a bounding box for orthographic view",
			pattern=TileNumber.PATTERN, longName="oview.tiles")
	List<TileNumber> getOviewTiles();
	boolean isOviewTiles();

	@Option(description="lat,lon,ele of camera position for perspective view",
			pattern= LatLonEle.PATTERN, longName="pview.pos")
	LatLonEle getPviewPos();
	boolean isPviewPos();

	@Option(description="lat,lon,ele of camera look-at for perspective view",
			pattern=LatLonEle.PATTERN, longName="pview.lookAt")
	LatLonEle getPviewLookat();
	boolean isPviewLookat();

	@Option(description="vertical field of view angle for perspective view, in degrees",
			longName="pview.fovy", defaultValue="45")
	double getPviewFovy();
	boolean isPviewFovy();

	@Option(description="aspect ratio (width / height) for perspective view",
			longName="pview.aspect")
	double getPviewAspect();
	boolean isPviewAspect();

	/* metadata */

	@Option(description="path to a JSON file with metadata, or an mbtiles file with such JSON data for multiple tiles")
	File getMetadataFile();
	boolean isMetadataFile();

	/* logging */

	@Option(description="output directory for log files")
	File getLogDir();
	boolean isLogDir();

	/* other parameters */

	@Option(description="start the graphical user interface")
	boolean getGui();

	@Option(helpRequest=true, description="show this help", shortName="?")
	boolean getHelp();

	@Option(description="print software version and exit")
	boolean getVersion();

	/* parameter files */

	@Option(description="a file containing one set of parameters per line")
	File getParameterFile();
	boolean isParameterFile();

	@Option(description="a directory containing parameter files; new files may be added while OSM2World is running")
	File getParameterFileDir();
	boolean isParameterFileDir();

}
