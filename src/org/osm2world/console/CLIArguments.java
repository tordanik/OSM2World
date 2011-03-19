package org.osm2world.console;

import java.io.File;
import java.util.List;

import org.osm2world.core.target.common.rendering.TileNumber;

import uk.co.flamingpenguin.jewel.cli.Option;

public interface CLIArguments {
	
	public static final String OUTPUT_PATTERN = "(.*)\\.(?:obj|pov|png)";
	
	/* input and output */
	
	@Option(description="the .osm input file", shortName="i")
	File getInput();
	boolean isInput();
	
	@Option(description="output files", shortName="o", pattern=OUTPUT_PATTERN)
	List<File> getOutput();
	boolean isOutput();
	
	@Option(description="output size in pixels", pattern=Resolution.PATTERN,
			defaultValue="800,600")
	Resolution getResolution();
	boolean isResolution();
	
	/* camera */
	
	@Option(description="downwards angle of orthographic view in degrees",
			longName="oview.angle", defaultValue="30")
	double getOviewAngle();
	boolean isOviewAngle();
	
	@Option(description="lat,lon pairs defining a bounding box for orthographic view",
			pattern=LatLonEle.PATTERN, longName="oview.bbox")
	List<LatLonEle> getOviewBoundingBox();
	boolean isOviewBoundingBox();
	
	@Option(description="zoom,x,y triples of tiles defining a bounding box for orthographic view",
			pattern=TileNumber.PATTERN, longName="oview.tiles")
	List<TileNumber> getOviewTiles();
	boolean isOviewTiles();
	
	@Option(description="lat,lon,ele of camera position for perspective view",
			pattern=LatLonEle.PATTERN_WITH_ELE, longName="pview.pos")
	LatLonEle getPviewPos();
	boolean isPviewPos();
	
	@Option(description="lat,lon,ele of camera look-at for perspective view",
			pattern=LatLonEle.PATTERN_WITH_ELE, longName="pview.lookAt")
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
	
	/* logging */
	
	@Option(description="writes execution times to the command line")
	boolean getPerformancePrint();
	
	@Option(description="appends a line with execution times to a file")
	File getPerformanceTable();
	boolean isPerformanceTable();
	
	/* other parameters */
	
	@Option(description="start the graphical user interface")
	boolean getGui();
	
	@Option(helpRequest=true, description="show this help", shortName="?")
	boolean getHelp();
	
	@Option(description="print software version and exit")
	boolean getVersion();
	
}
