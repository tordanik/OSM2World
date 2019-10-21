package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openstreetmap.josm.plugins.graphview.core.data.MapBasedTagGroup;
import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.test.TestMapDataGenerator;
import org.osm2world.core.world.modules.BuildingModule.BuildingPart;
import org.osm2world.core.world.modules.BuildingModule.Wall;

public class BuildingModuleTest {

	@Test
	public void testSplitIntoWalls() {

		TestMapDataGenerator generator = new TestMapDataGenerator();

		List<MapNode> nodes = new ArrayList<>(asList(
				generator.createNode(new VectorXZ(-10, -5)),
				generator.createNode(new VectorXZ(  0, -5)),
				generator.createNode(new VectorXZ(+10, -5)),
				generator.createNode(new VectorXZ(+10, +5)),
				generator.createNode(new VectorXZ(-10, +5))
				));

		nodes.add(nodes.get(0));

		MapArea buildingPartArea = generator.createWayArea(nodes, new MapBasedTagGroup(new Tag("building", "yes")));

		/* test the basic case */

		List<Wall> result = BuildingPart.splitIntoWalls(buildingPartArea, null);

		assertEquals(4, result.size());

		assertEquals(nodes.subList(0, 3), result.get(0).getNodes());
		assertEquals(nodes.subList(2, 4), result.get(1).getNodes());
		assertEquals(nodes.subList(3, 5), result.get(2).getNodes());
		assertEquals(nodes.subList(4, 6), result.get(3).getNodes());

		/* add a building:wall=yes way and test again */

		MapWay wallWay = generator.createWay(asList(nodes.get(1), nodes.get(0), nodes.get(4)),
				new MapBasedTagGroup(new Tag("building:wall", "yes")));

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
