package org.osm2world.console.commands.mixins;

import java.io.File;

import javax.annotation.Nullable;

import org.osm2world.math.geo.GeoBounds;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.osm.creation.*;

import picocli.CommandLine;

public class InputOptions {

	public enum InputMode {FILE, OVERPASS}

	@CommandLine.Option(names = {"--input_mode"}, description = "input mode (FILE or OVERPASS)",
			paramLabel = "<mode>", defaultValue = "FILE")
	public InputMode inputMode;

	@CommandLine.Option(names = {"--input", "-i"}, required = true, description = "input file with data in OSM format",
			paramLabel = "<path>")
	public File input;

	@CommandLine.Option(names = {"--input_query"}, description = "Overpass API query string", paramLabel = "<query>")
	@Nullable
	public String inputQuery = null;

	@CommandLine.Option(names = {"--overpass_url"}, description = "Overpass API instance to use",
			defaultValue = OverpassReader.DEFAULT_API_URL, paramLabel = "<url>")
	public String overpassURL;

	public OSMDataReaderView buildInput(@Nullable GeoBounds bounds) {

		OSMDataReader dataReader = switch (inputMode) {

			case FILE -> {
				File inputFile = input;
				String inputName = inputFile.getName();
				if (inputName.endsWith(".mbtiles")) {
					yield new MbtilesReader(inputFile);
				} else if (inputName.endsWith(".gol")) {
					yield new GeodeskReader(inputFile);
				} else {
					yield new OSMFileReader(inputFile);
				}
			}

			case OVERPASS -> new OverpassReader(overpassURL);

		};

		if (bounds instanceof TileNumber tile) {
			return new OSMDataReaderView(dataReader, tile);
		} else if (bounds != null) {
			return new OSMDataReaderView(dataReader, bounds.latLonBounds());
		} else if (inputQuery != null && dataReader instanceof OverpassReader overpassReader) {
			return new OSMDataReaderView(overpassReader, inputQuery);
		} else {
			return new OSMDataReaderView(dataReader);
		}

	}

}
