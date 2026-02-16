package org.osm2world.util.uri;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Implementation of {@link HttpUriImplementation} for use on the JVM.
 */
public class HttpUriImplementationJvm implements HttpUriImplementation {

	/** Sets up {@link LoadUriUtil} to use this implementation. */
	public static void register() {
		LoadUriUtil.setImplementation(new HttpUriImplementationJvm());
	}

	@Override
	public String fetchText(URI uri) throws IOException {
		try {
			try (InputStream is = uri.toURL().openStream();
				 BufferedReader reader = new BufferedReader(
						 new InputStreamReader(is, StandardCharsets.UTF_8))) {
				return reader.lines().collect(Collectors.joining("\n"));
			}
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public byte[] fetchBinary(URI uri) throws IOException {
		try {
			try (InputStream is = uri.toURL().openStream();
			     var baos = new ByteArrayOutputStream()) {
				byte[] buffer = new byte[8192];
				int bytesRead;
				while ((bytesRead = is.read(buffer)) != -1) {
					baos.write(buffer, 0, bytesRead);
				}
				return baos.toByteArray();
			}
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public boolean checkExists(URI uri) {
		try {
			HttpURLConnection huc = (HttpURLConnection) uri.toURL().openConnection();
			huc.setRequestMethod("HEAD");
			if (huc.getResponseCode() == HttpURLConnection.HTTP_OK) {
				return true;
			}
		} catch (IOException ignored) {}
		return false;
	}

}