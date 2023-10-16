package org.osm2world.console;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

public class OSM2WorldTest {

	@Test
	public void testLoadConfigFiles() throws ConfigurationException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File config01 = new File(classLoader.getResource("testConfig_01.properties").getFile());
		File config02 = new File(classLoader.getResource("testConfig_02.properties").getFile());

		Configuration result = OSM2World.loadConfigFiles(null, config01, config02);

		assertEquals(0.02, result.getDouble("treesPerSquareMeter"), 0);
		assertEquals("foobar", result.getString("stringProperty"));
		assertEquals(false, result.getBoolean("keepOsmElements"));

	}

}