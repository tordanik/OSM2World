package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
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

}
