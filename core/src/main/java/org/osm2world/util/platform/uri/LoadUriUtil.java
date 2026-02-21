package org.osm2world.util.platform.uri;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.regex.Pattern;

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
			return new String(decodeDataUri(uri));
		}

		return implementation.fetchText(uri);

	}

	public static byte[] fetchBinary(URI uri) throws IOException {

		if (uri.getScheme().equals("data")) {
			return decodeDataUri(uri);
		}

		return implementation.fetchBinary(uri);

	}

	private static byte[] decodeDataUri(URI uri) throws IOException {
		String s = uri.getSchemeSpecificPart();
		var pattern = Pattern.compile("([^,]+),(.+)");
		var matcher = pattern.matcher(s);
		if (matcher.matches()) {
			if (matcher.group(1).endsWith(";base64")) {
				return Base64.getDecoder().decode(matcher.group(2));
			} else {
				return matcher.group(2).getBytes();
			}
		} else {
			throw new IOException("Not a valid data URI, no match found: "
				+ ((s.length() < 500) ? s : s.substring(0, 500) + "..."));
		}
	}

}
