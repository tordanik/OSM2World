package org.osm2world.map_data.data;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.*;

import org.junit.Test;

public class TagSetTest {

	@Test
	public void testOfTags() {

		TagSet t0 = TagSet.of(emptyList());
		assertTrue(t0.isEmpty());
		assertEquals(0, t0.size());

		TagSet t1 = TagSet.of(new Tag("landuse", "residential"));
		assertFalse(t1.isEmpty());
		assertEquals(1, t1.size());
		assertTrue(t1.containsKey("landuse"));
		assertTrue(t1.containsValue("residential"));

		TagSet t3 = TagSet.of(
				"highway", "crossing",
				"crossing", "uncontrolled",
				"kerb", "lowered");
		assertFalse(t3.isEmpty());
		assertEquals(3, t3.size());


	}

	@Test
	public void testOfKeyValuePairs() {

		TagSet t0 = TagSet.of();
		assertTrue(t0.isEmpty());
		assertEquals(0, t0.size());

		TagSet t1 = TagSet.of("amenity", "place_of_worship");
		assertFalse(t1.isEmpty());
		assertEquals(1, t1.size());

		TagSet t3 = TagSet.of(
				"highway", "crossing",
				"crossing", "uncontrolled",
				"kerb", "lowered");
		assertFalse(t3.isEmpty());
		assertEquals(3, t3.size());

	}

	@Test
	public void testEquals() {

		assertEquals(TagSet.of(), TagSet.of());

		assertEquals(TagSet.of("keyA", "valueA", "keyB", "valueB"),
				TagSet.of("keyB", "valueB", "keyA", "valueA"));

		assertNotEquals(TagSet.of("keyA", "valueA", "keyB", "valueB"),
				TagSet.of("keyA", "valueB", "keyB", "valueA"));

	}

	@Test(expected = IllegalArgumentException.class)
	public void testUniqueness() {
		TagSet.of("highway", "primary", "highway", "secondary");
	}

	@Test
	public void testContainsAny() {

		TagSet set = TagSet.of(
				"highway", "crossing",
				"crossing", "uncontrolled",
				"kerb", "lowered");

		assertTrue(set.containsAny(null, null));

		assertTrue(set.containsAny(null, asList("residential", "uncontrolled")));
		assertFalse(set.containsAny(null, asList("foo", "bar")));

		assertTrue(set.containsAny(asList("highway", "amenity", "kerb"), null));
		assertFalse(set.containsAny(asList("1", "2"), null));

		assertTrue(set.containsAny(asList("amenity", "crossing"), asList("uncontrolled", "wood")));
		assertFalse(set.containsAny(asList("highway", "crossing"), asList("lowered")));

	}

}
