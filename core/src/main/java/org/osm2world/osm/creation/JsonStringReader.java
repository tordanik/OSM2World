package org.osm2world.osm.creation;

import java.io.IOException;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

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
	protected JsonValue getJsonRoot() throws IOException {
		return Json.parse(json);
	}

}
