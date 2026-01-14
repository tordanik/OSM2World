package org.osm2world.util.json;

import java.io.IOException;
import java.io.Writer;

import com.google.gson.GsonBuilder;

public class JsonImplementationJvm implements JsonImplementation {

	@Override
	public void serialize(Object obj, Writer writer, boolean prettyPrinting) throws IOException {
		var gsonBuilder = new GsonBuilder();
		if (prettyPrinting) {
			gsonBuilder.setPrettyPrinting();
		}
		gsonBuilder.create().toJson(obj, writer);
	}

}
