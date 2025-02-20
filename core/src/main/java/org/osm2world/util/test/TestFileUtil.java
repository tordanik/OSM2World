package org.osm2world.util.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.annotation.Nonnull;

public final class TestFileUtil {

	/** prevents instantiation */
	private TestFileUtil() {}

	/** loads a test file as a resource */
	public static @Nonnull File getTestFile(String name) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		var resource = classLoader.getResource(name);
		if (resource == null) throw new AssertionError("Test file '" + name + "' not found");
		return new File(resource.getFile());
	}

	/** creates a temporary file for use in tests */
	public static File createTempFile(String prefix, String suffix) {
		try {
			var tempFile = Files.createTempFile(prefix, suffix).toFile();
			tempFile.deleteOnExit();
			return tempFile;
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/** creates a temporary file for use in tests */
	public static File createTempFile(String suffix) {
		return createTempFile("o2w-test-", suffix);
	}

}
