package org.osm2world.console;

import static org.junit.Assert.assertTrue;
import static org.osm2world.util.test.TestFileUtil.createTempFile;
import static org.osm2world.util.test.TestFileUtil.getTestFile;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

public class OSM2WorldTest {

	@Test
	public void testMain() throws Exception {

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

}