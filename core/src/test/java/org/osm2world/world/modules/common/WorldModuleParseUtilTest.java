package org.osm2world.world.modules.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.osm2world.util.enums.UpDown.DOWN;
import static org.osm2world.util.enums.UpDown.UP;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.parseInclineDirection;

import org.junit.Test;
import org.osm2world.map_data.data.Tag;
import org.osm2world.map_data.data.TagSet;

public class WorldModuleParseUtilTest {

	@Test
	public void testParseInclineDirection() {

		assertEquals(UP, parseInclineDirection(TagSet.of("incline", "up")));
		assertEquals(UP, parseInclineDirection(TagSet.of("incline", "10%")));

		assertEquals(DOWN, parseInclineDirection(TagSet.of("incline", "down")));
		assertEquals(DOWN, parseInclineDirection(TagSet.of("incline", "-5 %")));

		assertNull(parseInclineDirection(TagSet.of("incline", "0")));
		assertNull(parseInclineDirection(TagSet.of()));

	}

	@Test
	public void testInheritTags() {

		TagSet ownTags = TagSet.of(
				new Tag("key0", "valA"),
				new Tag("key1", "valB"));

		TagSet parentTags = TagSet.of(
				new Tag("key1", "valX"),
				new Tag("key2", "valY"));

		TagSet result = WorldModuleParseUtil.inheritTags(ownTags, parentTags);

		assertEquals(3, result.size());
		assertEquals("valA", result.getValue("key0"));
		assertEquals("valB", result.getValue("key1"));
		assertEquals("valY", result.getValue("key2"));

	}

}
