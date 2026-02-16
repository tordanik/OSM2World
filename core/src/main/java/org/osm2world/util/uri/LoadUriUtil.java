package org.osm2world.util.uri;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Loads content from {@link java.net.URI}s.
 * Supports file, http(s) and data URIs.
 * Internally uses separate implementations for use on the browser and on JVM.
 */
public class LoadUriUtil {

	private static HttpUriImplementation implementation = null;

	/**
	 * Globally sets a different {@link HttpUriImplementation}.
	 * This exists to support in-browser use.
	 * Most code should never call this method.
	 */
	static void setImplementation(HttpUriImplementation implementation) {
		LoadUriUtil.implementation = implementation;
	}

	public static boolean checkExists(URI uri) {

		if (uri.getScheme().equals("data")) {
			return true;
		} else if (uri.getScheme().equals("file")) {
			return new File(uri).exists();
		} else {
			return implementation.checkExists(uri);
		}

	}

	public static String fetchText(URI uri) throws IOException {

		if (uri.getScheme().equals("data")) {
			// TODO implement data uris
			throw new UnsupportedOperationException("data URIs are not implemented yet");
		}

		return implementation.fetchText(uri);

	}

	public static byte[] fetchBinary(URI uri) throws IOException {

		if (uri.getScheme().equals("data")) {
			// TODO implement data uris
			throw new UnsupportedOperationException("data URIs are not implemented yet");
		}

		return implementation.fetchBinary(uri);

	}

}
