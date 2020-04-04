package org.osm2world.core.map_data.data;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;

import org.junit.Test;

public class TagGroupTest {

	@Test
	public void testOfTags() {

		TagGroup t0 = TagGroup.of(emptyList());
		assertTrue(t0.isEmpty());
		assertEquals(0, t0.size());

		TagGroup t1 = TagGroup.of(new Tag("landuse", "residential"));
		assertFalse(t1.isEmpty());
		assertEquals(1, t1.size());
		assertTrue(t1.containsKey("landuse"));
		assertTrue(t1.containsValue("residential"));

	}

	@Test
	public void testOfKeyValuePairs() {

		TagGroup t0 = TagGroup.of();
		assertTrue(t0.isEmpty());
		assertEquals(0, t0.size());

		TagGroup t1 = TagGroup.of("amenity", "place_of_worship");
		assertFalse(t1.isEmpty());
		assertEquals(1, t1.size());

		TagGroup t3 = TagGroup.of(
				"highway", "crossing",
				"crossing", "uncontrolled",
				"kerb", "lowered");
		assertFalse(t3.isEmpty());
		assertEquals(3, t3.size());

	}

}
