package org.osm2world.util.json;

import java.io.IOException;
import java.io.Writer;

/**
 * implementation of the functionality exposed through JsonUtil
 */
public interface JsonImplementation {

	void serialize(Object obj, Writer writer, boolean prettyPrinting) throws IOException;

}
