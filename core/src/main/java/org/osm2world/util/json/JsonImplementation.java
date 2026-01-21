package org.osm2world.util.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.apache.commons.io.IOUtils;

/**
 * implementation of the functionality exposed through JsonUtil
 */
public interface JsonImplementation {

	void serialize(Object obj, Writer writer, boolean prettyPrinting) throws IOException;

	<T> T deserialize(String json, Class<T> type) throws IOException;

	default <T> T deserialize(Reader reader, Class<T> type) throws IOException {
		String json = IOUtils.toString(reader);
		return deserialize(json, type);
	}

}
