package org.osm2world.util.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Utility class for JSON serialization and deserialization.
 * Internally uses separate implementations for use on the browser and on JVM.
 */
public class JsonUtil {

	private static JsonImplementation implementation = null;

	/**
	 * Globally sets a different {@link JsonImplementation}.
	 * This exists to support in-browser use.
	 * Most code should never call this method.
	 */
	static void setImplementation(JsonImplementation implementation) {
		JsonUtil.implementation = implementation;
	}

	public static void toJson(Object obj, Writer writer, boolean prettyPrinting) throws IOException {
		implementation.toJson(obj, writer, prettyPrinting);
	}

	public static <T> T fromJson(String json, Class<T> type) throws IOException {
		return implementation.fromJson(json, type);
	}

	public static <T> T fromJson(Reader reader, Class<T> type) throws IOException {
		return implementation.fromJson(reader, type);
	}

}
