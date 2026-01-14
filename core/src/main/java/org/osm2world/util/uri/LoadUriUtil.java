package org.osm2world.util.uri;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.osm2world.util.functions.Factory;

/**
 * Loads content from {@link java.net.URI}s.
 * Supports file, http(s) and data URIs.
 * Internally uses separate implementations for use on the browser and on JVM.
 */
public class LoadUriUtil {

	private static Factory<HttpClient> clientFactory = JvmHttpClient::new;

	/**
	 * Globally sets a different {@link HttpClient} implementation.
	 * This exists to support in-browser use.
	 * Most code should never call this.
	 */
	public static void setClientFactory(Factory<HttpClient> clientFactory) {
		LoadUriUtil.clientFactory = clientFactory;
	}

	public static boolean checkExists(URI uri) {

		if (uri.getScheme().equals("data")) {
			return true;
		} else if (uri.getScheme().equals("file")) {
			return new File(uri).exists();
		} else {
			HttpClient client = clientFactory.get();
			return client.checkExists(uri);
		}

	}

	public static String fetchText(URI uri) throws IOException {

		if (uri.getScheme().equals("data")) {
			// TODO implement data uris
			throw new UnsupportedOperationException("data URIs are not implemented yet");
		}

		HttpClient client = clientFactory.get();
		return client.fetchText(uri);

	}

	public static byte[] fetchBinary(URI uri) throws IOException {

		if (uri.getScheme().equals("data")) {
			// TODO implement data uris
			throw new UnsupportedOperationException("data URIs are not implemented yet");
		}

		HttpClient client = clientFactory.get();
		return client.fetchBinary(uri);

	}

}
