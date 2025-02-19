package org.osm2world.console.commands.mixins;

import java.io.File;

import org.osm2world.osm.creation.OverpassReader;

import picocli.CommandLine;

public class InputOptions {

	public enum InputMode {FILE, OVERPASS}

	@CommandLine.Option(names = {"--input_mode"}, description = "input mode", defaultValue = "FILE")
	public InputMode inputMode;

	@CommandLine.Option(names = {"--input", "-i"}, required = true, description = "input file with data in OSM format")
	public File input;

	@CommandLine.Option(names = {"--overpass_url"}, description = "overpass instance to use",
			defaultValue = OverpassReader.DEFAULT_API_URL)
	public String overpassURL;

}
