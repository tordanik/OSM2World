package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.osm2world.core.world.modules.RoadModule.findMatchingLanes;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.world.modules.RoadModule.Lane;
import org.osm2world.core.world.modules.RoadModule.RoadPart;

public class RoadModuleTest {

	@Test
	public void testFindMatchingLanes() {

		List<Lane> lanes1 = asList(
				new Lane(null, RoadModule.VEHICLE_LANE, RoadPart.LEFT, TagSet.of()),
				new Lane(null, RoadModule.KERB, RoadPart.LEFT, TagSet.of()),
				new Lane(null, RoadModule.SIDEWALK, RoadPart.LEFT, TagSet.of()));
		List<Lane> lanes2 = asList(
				new Lane(null, RoadModule.KERB, RoadPart.LEFT, TagSet.of()),
				new Lane(null, RoadModule.SIDEWALK, RoadPart.LEFT, TagSet.of()));

		Map<Integer, Integer> result = findMatchingLanes(lanes1, lanes2, true, false);

		assertEquals(2, result.size());
		assertEquals(0, (int)result.get(1));
		assertEquals(1, (int)result.get(2));

	}

	@Test
	public void testGetTagsWithPrefix() {

		TagSet tags = TagSet.of(
				"highway", "tertiary",
				"sidewalk", "both",
				"sidewalk:left:width", "2 m",
				"sidewalk:right:width", "3 m",
				"sidewalk:left:kerb", "lowered",
				"sidewalk:left:kerb:width", "0.30");

		TagSet sidewalkResult = RoadModule.Road.getTagsWithPrefix(tags, "sidewalk:left:", null);

		assertEquals(3, sidewalkResult.size());
		assertTrue(sidewalkResult.contains("width", "2 m"));
		assertTrue(sidewalkResult.contains("kerb", "lowered"));
		assertTrue(sidewalkResult.contains("kerb:width", "0.30"));

		TagSet kerbResult = RoadModule.Road.getTagsWithPrefix(tags, "sidewalk:left:kerb", "kerb");

		assertEquals(2, kerbResult.size());
		assertTrue(kerbResult.contains("kerb", "lowered"));
		assertTrue(kerbResult.contains("kerb:width", "0.30"));

	}

}
