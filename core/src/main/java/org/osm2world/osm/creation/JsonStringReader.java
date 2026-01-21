package org.osm2world.osm.creation;

import java.io.IOException;

/**
 * implementation of {@link JsonReader} which reads OSM JSON from a JSON string
 * @see JsonReader
 */
public class JsonStringReader extends JsonReader {

	private final String json;

	public JsonStringReader(String json) {
		this.json = json;
	}

	@Override
	protected String getJsonString() throws IOException {
		return json;
	}

}
