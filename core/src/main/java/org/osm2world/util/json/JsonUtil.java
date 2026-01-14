package org.osm2world.util.json;

import java.io.IOException;
import java.io.Writer;

/**
 * Utility class for JSON serialization and deserialization.
 * Internally uses separate implementations for use on the browser and on JVM.
 */
public class JsonUtil {

	private static JsonImplementation implementation = new JsonImplementationJvm();

	/**
	 * Globally sets a different {@link JsonImplementation}.
	 * This exists to support in-browser use.
	 * Most code should never call this.
	 */
	public static void setImplementation(JsonImplementation implementation) {
		JsonUtil.implementation = implementation;
	}

	public static void serialize(Object obj, Writer writer, boolean prettyPrinting) throws IOException {
		implementation.serialize(obj, writer, prettyPrinting);
	}

}
