package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openstreetmap.josm.plugins.graphview.core.data.MapBasedTagGroup;
import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.test.TestMapDataGenerator;
import org.osm2world.core.world.modules.BuildingModule.BuildingPart;

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

		List<List<MapNode>> result = BuildingPart.splitIntoWalls(buildingPartArea, emptyList());

		assertEquals(4, result.size());

		assertEquals(nodes.subList(0, 3), result.get(0));
		assertEquals(nodes.subList(2, 4), result.get(1));
		assertEquals(nodes.subList(3, 5), result.get(2));
		assertEquals(nodes.subList(4, 6), result.get(3));

	}

}
