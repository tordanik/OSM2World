package org.osm2world.map_data.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Assert;
import org.junit.Test;
import org.osm2world.map_data.data.overlaps.MapElementId;

public class MapElementIdTest {

	@Test
	public void testParse() {

		var mapId01 = MapElementId.parse("n123");
		Assert.assertNotNull(mapId01);
		assertEquals(MapElementId.ElementType.NODE, mapId01.type());
		assertEquals(123L, mapId01.getId());

		var mapId02 = MapElementId.parse("w-99");
		Assert.assertNotNull(mapId02);
		assertEquals(MapElementId.ElementType.WAY, mapId02.type());
		assertEquals(-99L, mapId02.getId());

		var mapId03 = MapElementId.parse("R0");
		Assert.assertNotNull(mapId03);
		assertEquals(MapElementId.ElementType.RELATION, mapId03.type());
		assertEquals(0L, mapId03.getId());

	}

	@Test
	public void testParse_invalid() {

		assertNull(MapElementId.parse("node123"));

	}

}
