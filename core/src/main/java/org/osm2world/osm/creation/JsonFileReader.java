package org.osm2world.osm.creation;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

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
	protected String getJsonString() throws IOException {
		return IOUtils.toString(new FileReader(jsonFile));
	}

}
