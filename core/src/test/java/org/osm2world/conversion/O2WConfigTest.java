package org.osm2world.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osm2world.util.test.TestFileUtil.getTestFile;

import java.io.File;
import java.net.URI;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.osm2world.scene.color.Color;
import org.osm2world.util.enums.ForwardBackward;
import org.osm2world.util.enums.LeftRight;
import org.osm2world.util.enums.LeftRightBoth;

public class O2WConfigTest {

	@Test
	public void testLoadConfigFiles() {

		File config01 = getTestFile("config/testConfig_01.properties");
		File config02 = getTestFile("config/testConfig_02.properties");

		O2WConfig result = new O2WConfig(null, config01, config02);

		assertEquals(0.02, result.getDouble("treesPerSquareMeter", 0.42), 0);
		assertEquals("foobar", result.getString("stringProperty"));
		assertTrue(result.getBoolean("keepOsmElements"));

		result = result.withProperty("treesPerSquareMeter", null);
		result = result.withProperty("stringProperty", "baz");

		assertEquals(0.42, result.getDouble("treesPerSquareMeter", 0.42), 0);
		assertEquals("baz", result.getString("stringProperty", "something"));
		assertTrue(result.getBoolean("keepOsmElements"));

		URI textureURI = result.resolveFileConfigProperty("textures/test.png");
		assertTrue(textureURI != null && new File(textureURI).exists());

	}

	@Test
	public void testKeys() {

		File config01 = getTestFile("config/testConfig_01.properties");
		O2WConfig result = new O2WConfig(null, config01);

		Set<String> keys = new HashSet<>(result.getKeys());
		keys.remove("configBaseURI");

		assertEquals(Set.of("treesPerSquareMeter", "keepOsmElements"), keys);

	}

	@Test
	public void testLoadIncludedConfigFiles() {

		File parentConfig = getTestFile("config/parentConfig.properties");

		O2WConfig result = new O2WConfig(null, parentConfig);

		assertEquals(0.02, result.getDouble("treesPerSquareMeter", 0), 0);
		assertEquals("foobar", result.getString("stringProperty"));
		assertEquals(3.14f, result.getFloat("parentProperty", 0), 0);
		assertTrue(result.getBoolean("keepOsmElements"));

		result = result.withProperty("treesPerSquareMeter", null);
		result = result.withProperty("stringProperty", "baz");

		assertEquals(0.42, result.getDouble("treesPerSquareMeter", 0.42), 0);
		assertEquals("baz", result.getString("stringProperty", "something"));
		assertTrue(result.getBoolean("keepOsmElements"));

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

	@Test
	public void testConsoleLogLevels() {

		var c1 = new O2WConfig(Map.of("consoleLogLevels", "ERROR; FATAL"));
		assertEquals(EnumSet.of(ConversionLog.LogLevel.ERROR, ConversionLog.LogLevel.FATAL), c1.consoleLogLevels());

		var c2 = new O2WConfig(Map.of("consoleLogLevels", "WARNING"));
		assertEquals(EnumSet.of(ConversionLog.LogLevel.WARNING), c2.consoleLogLevels());

	}

}