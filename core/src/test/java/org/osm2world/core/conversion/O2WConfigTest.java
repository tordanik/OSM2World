package org.osm2world.core.conversion;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class O2WConfigTest {

	@Test
	public void testLoadConfigFiles() {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File config01 = new File(classLoader.getResource("config/testConfig_01.properties").getFile());
		File config02 = new File(classLoader.getResource("config/testConfig_02.properties").getFile());

		O2WConfig result = new O2WConfig(null, config01, config02);

		assertEquals(0.02, result.getDouble("treesPerSquareMeter", 0.42), 0);
		assertEquals("foobar", result.getString("stringProperty"));
		assertFalse(result.getBoolean("keepOsmElements"));

		result = result.withProperty("treesPerSquareMeter", null);
		result = result.withProperty("stringProperty", "baz");

		assertEquals(0.42, result.getDouble("treesPerSquareMeter", 0.42), 0);
		assertEquals("baz", result.getString("stringProperty", "something"));
		assertFalse(result.getBoolean("keepOsmElements"));

		File texturePath = ConfigUtil.resolveFileConfigProperty(result, "textures/test.png");
		assertTrue(texturePath.exists());

	}

	@Test
	public void testLoadIncludedConfigFiles() {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File parentConfig = new File(classLoader.getResource("config/parentConfig.properties").getFile());

		O2WConfig result = new O2WConfig(null, parentConfig);

		assertEquals(0.02, result.getDouble("treesPerSquareMeter", 0), 0);
		assertEquals("foobar", result.getString("stringProperty"));
		assertEquals(3.14f, result.getFloat("parentProperty", 0), 0);
		assertFalse(result.getBoolean("keepOsmElements"));

		result = result.withProperty("treesPerSquareMeter", null);
		result = result.withProperty("stringProperty", "baz");

		assertEquals(0.42, result.getDouble("treesPerSquareMeter", 0.42), 0);
		assertEquals("baz", result.getString("stringProperty", "something"));
		assertFalse(result.getBoolean("keepOsmElements"));

	}

}