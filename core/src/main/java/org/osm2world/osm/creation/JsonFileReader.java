package org.osm2world.osm.creation;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

/**
 * implementation of {@link JsonReader} which reads OSM JSON from a file
 * @see JsonReader
 */
public class JsonFileReader extends JsonReader {

	private final File jsonFile;

	public JsonFileReader(File jsonFile) {
		this.jsonFile = jsonFile;
	}

	@Override
	protected JsonValue getJsonRoot() throws IOException {
		return Json.parse(new FileReader(jsonFile));
	}

}
