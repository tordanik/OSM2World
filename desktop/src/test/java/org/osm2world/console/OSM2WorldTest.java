package org.osm2world.console;

import static org.junit.Assert.*;
import static org.osm2world.util.test.TestFileUtil.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;
import org.osm2world.util.test.TestFileUtil;

public class OSM2WorldTest {

	private static final int NUM_PARAM_DIR_TEST_FILES = 25;

	@Test
	public void testConvert() throws Exception {

		for (String ext : List.of("obj", "gltf", "glb", "pov", "o2w.pbf", "png", "ppm", "gd")) {

			for (boolean legacy : List.of(false, true)) {

				File inputFile = getTestFile("testWall.osm");
				File outputFile = createTempFile("." + ext);

				var options = List.of(
						"convert",
						"-i", inputFile.getAbsolutePath(),
						"-o", outputFile.getAbsolutePath()
				);

				if (legacy) {
					options = options.subList(1, options.size());
				}

				OSM2World.main(options.toArray(new String[0]), false);

				assertTrue(Files.size(outputFile.toPath()) > 0);

			}

		}

	}

	@Test
	public void testParams_Files() throws Exception {

		File inputFile = getTestFile("testWall.osm");

		File[] outputFiles = Stream.of("_0.glb", "_1.gltf", "_2.obj")
				.map(TestFileUtil::createTempFile)
				.map(File::getAbsoluteFile)
				.toArray(File[]::new);

		File paramsFile1 = createTempFile("params1", ".txt");
		Files.writeString(paramsFile1.toPath(),
				"convert --input " + inputFile + " --output " + outputFiles[0]);

		File paramsFile2 = createTempFile("params2", ".txt");
		Files.writeString(paramsFile2.toPath(),
				"# Test with multiple lines\n "
						+ "\n-i " + inputFile + " -o " + outputFiles[1]
						+ "\n-i " + inputFile + " -o " + outputFiles[2]);

		OSM2World.main(new String[]{"params", paramsFile1.toString(), paramsFile2.toString()}, false);

		for (File outputFile : outputFiles) {
			assertTrue(outputFile + " contains no data", Files.size(outputFile.toPath()) > 0);
		}

	}

	@Test
	public void testParams_Dir_keep() throws Exception {

		Path paramFileDir = testParams_Dir(false);

		File[] paramFiles = paramFileDir.toFile().listFiles();
		assertNotNull(paramFiles);
		assertEquals(NUM_PARAM_DIR_TEST_FILES, paramFiles.length);

	}

	@Test
	public void testParams_Dir_delete() throws Exception {

		Path paramFileDir = testParams_Dir(true);

		File[] paramFiles = paramFileDir.toFile().listFiles();
		assertNotNull(paramFiles);
		assertEquals(0, paramFiles.length);

	}

	public Path testParams_Dir(boolean delete) throws Exception {

		File inputFile = getTestFile("testWall.osm");

		File[] outputFiles = IntStream.range(0, NUM_PARAM_DIR_TEST_FILES)
				.mapToObj(it -> "_"  + it + ".glb.gz")
				.map(TestFileUtil::createTempFile)
				.map(File::getAbsoluteFile)
				.toArray(File[]::new);

		Path paramFileDir = createTempDirectory().toPath();

		for (File outputFile : outputFiles) {
			Path paramsFile = paramFileDir.resolve(outputFile.getName());
			Files.writeString(paramsFile,
					"-i " + inputFile + " -o " + outputFile);
		}

		String[] args = delete
				? new String[]{ "params", paramFileDir.toString(), "--deleteProcessedFiles" }
				: new String[]{ "params", paramFileDir.toString() };
		OSM2World.main(args, false);

		for (File outputFile : outputFiles) {
			assertTrue(outputFile  + " contains no data", Files.size(outputFile.toPath()) > 0);
		}

		return paramFileDir;

	}

}