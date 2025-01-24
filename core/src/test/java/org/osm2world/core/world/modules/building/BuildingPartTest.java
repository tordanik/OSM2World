package org.osm2world.core.world.modules.building;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.osm2world.core.math.GeometryUtil.closeLoop;

import java.util.List;

import org.junit.Test;
import org.osm2world.core.map_data.creation.MapDataBuilder;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.TagSet;

public class BuildingPartTest {

	@Test
	public void testSplitIntoWalls() {

		var builder = new MapDataBuilder();

		List<MapNode> nodes = closeLoop(
				builder.createNode(-10, -5),
				builder.createNode(  0, -5),
				builder.createNode(+10, -5),
				builder.createNode(+10, +5),
				builder.createNode(-10, +5));

		MapArea buildingPartArea = builder.createWayArea(nodes, TagSet.of("building", "yes"));

		/* test the basic case */

		List<ExteriorBuildingWall> result = BuildingPart.splitIntoWalls(buildingPartArea, null);

		assertEquals(4, result.size());

		assertEquals(nodes.subList(0, 3), result.get(0).getNodes());
		assertEquals(nodes.subList(2, 4), result.get(1).getNodes());
		assertEquals(nodes.subList(3, 5), result.get(2).getNodes());
		assertEquals(nodes.subList(4, 6), result.get(3).getNodes());

		/* add a building:wall=yes way and test again */

		MapWay wallWay = builder.createWay(asList(nodes.get(1), nodes.get(0), nodes.get(4)),
				TagSet.of("building:wall", "yes"));

		result = BuildingPart.splitIntoWalls(buildingPartArea, null);

		assertEquals(5, result.size());

		assertEquals(nodes.subList(0, 2), result.get(0).getNodes());
		assertEquals(wallWay, result.get(0).wallWay);
		assertEquals(nodes.subList(1, 3), result.get(1).getNodes());
		assertNull(result.get(1).wallWay);
		assertEquals(nodes.subList(2, 4), result.get(2).getNodes());
		assertNull(result.get(2).wallWay);
		assertEquals(nodes.subList(3, 5), result.get(3).getNodes());
		assertNull(result.get(3).wallWay);
		assertEquals(nodes.subList(4, 6), result.get(4).getNodes());
		assertEquals(wallWay, result.get(4).wallWay);

	}

}
