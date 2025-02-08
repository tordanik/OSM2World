package org.osm2world.conversion;

import static org.junit.Assert.*;

import java.awt.*;
import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.osm2world.util.enums.ForwardBackward;
import org.osm2world.util.enums.LeftRight;
import org.osm2world.util.enums.LeftRightBoth;

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

		File texturePath = result.resolveFileConfigProperty("textures/test.png");
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

	@Test
	public void testGetEnum() {

		var config = new O2WConfig(Map.of(
				"key1", "LEFT",
				"key2", "BACKWARD"
		));

		assertEquals(LeftRight.LEFT, config.getEnum(LeftRight.class, "key1"));
		assertEquals(LeftRightBoth.LEFT, config.getEnum(LeftRightBoth.class, "key1"));
		assertEquals(ForwardBackward.BACKWARD, config.getEnum(ForwardBackward.class, "key2"));

	}

	@Test
	public void testGetColor() {

		var config = new O2WConfig(Map.of(
				"key1", "#00FF00"
		));

		assertEquals(new Color(0, 255, 0), config.getColor("key1"));
		assertEquals(Color.RED, config.getColor("no_such_key", Color.RED));

	}

}