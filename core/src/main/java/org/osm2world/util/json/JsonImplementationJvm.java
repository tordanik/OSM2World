package org.osm2world.util.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonImplementationJvm implements JsonImplementation {

	@Override
	public void toJson(Object obj, Writer writer, boolean prettyPrinting) throws IOException {
		var gsonBuilder = new GsonBuilder();
		if (prettyPrinting) {
			gsonBuilder.setPrettyPrinting();
		}
		gsonBuilder.create().toJson(obj, writer);
	}

	@Override
	public <T> T fromJson(String json, Class<T> type) throws IOException {
		return new Gson().fromJson(json, type);
	}

	@Override
	public <T> T fromJson(Reader reader, Class<T> type) throws IOException {
		return new Gson().fromJson(reader, type);
	}

}
