package org.osm2world.util.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.apache.commons.io.IOUtils;

/**
 * Implementation of the functionality exposed through {@link JsonUtil}.
 * See that class for documentation.
 */
interface JsonImplementation {

	void toJson(Object obj, Writer writer, boolean prettyPrinting) throws IOException;

	<T> T fromJson(String json, Class<T> type) throws IOException;

	default <T> T fromJson(Reader reader, Class<T> type) throws IOException {
		String json = IOUtils.toString(reader);
		return fromJson(json, type);
	}

}
