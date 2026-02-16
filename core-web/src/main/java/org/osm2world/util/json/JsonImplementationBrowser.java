package org.osm2world.util.json;

import java.io.IOException;
import java.io.Writer;

import org.teavm.flavour.json.JSON;
import org.teavm.flavour.json.tree.Node;

/**
 * {@link JsonImplementation} for use in the browser.
 */
public class JsonImplementationBrowser implements JsonImplementation {

	/** Sets up {@link JsonUtil} to use this implementation. */
	public static void register() {
		JsonUtil.setImplementation(new JsonImplementationBrowser());
	}

	@Override
	public void toJson(Object obj, Writer writer, boolean prettyPrint) throws IOException {
		String json = JSON.serialize(obj).stringify();
		writer.write(json);
		writer.flush();
	}

	@Override
	public <T> T fromJson(String json, Class<T> type) {
		return JSON.deserialize(Node.parse(json), type);
	}

}
