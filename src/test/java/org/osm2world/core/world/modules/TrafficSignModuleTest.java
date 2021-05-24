package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.osm2world.core.world.modules.TrafficSignModule.findClosestJunction;

import java.util.List;

import org.junit.Test;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.test.TestMapDataGenerator;
import org.osm2world.core.world.modules.RoadModule.Road;

public class TrafficSignModuleTest {

	@Test
	public void zeroConnectedJunctions() throws Exception {

		TestMapDataGenerator generator = new TestMapDataGenerator();

		/* prepare fake map data */

		MapNode node00 = generator.createNode(0, 0);
		MapNode node01 = generator.createNode(1, 1);
		MapNode node02 = generator.createNode(2, 2);
		MapNode node03 = generator.createNode(3, 3);

		/* create a way and roads objects */

		List<MapNode> wayNodes = asList(node00, node01, node02, node03);
		MapWay way = generator.createWay(wayNodes, TagSet.of("highway", "tertiary"));
		way.getWaySegments().forEach(s -> s.addRepresentation(new Road(s)));

		/* check that no junction is found for any starting node */

		assertNull(findClosestJunction(node00));
		assertNull(findClosestJunction(node01));
		assertNull(findClosestJunction(node02));
		assertNull(findClosestJunction(node03));

	}

	@Test
	public void oneConnectedJunction() throws Exception {

		TestMapDataGenerator generator = new TestMapDataGenerator();

		/* prepare fake map data */

		MapNode node00 = generator.createNode(0, 0);
		MapNode node01 = generator.createNode(1, 1);
		MapNode node02 = generator.createNode(2, 2);
		MapNode intersectionNode = generator.createNode(3, 3);

		MapNode node10 = generator.createNode(3, 1);
		MapNode node20 = generator.createNode(4, 3);

		List<List<MapNode>> wayNodeLists = asList(
				asList(node00, node01, node02, intersectionNode),
				asList(intersectionNode, node10),
				asList(intersectionNode, node20));

		/* create ways and roads objects */

		for (List<MapNode> wayNodes : wayNodeLists) {
			MapWay way = generator.createWay(wayNodes, TagSet.of("highway", "tertiary"));
			way.getWaySegments().forEach(s -> s.addRepresentation(new Road(s)));
		}

		/* check that the single junction node is found for each starting node */

		assertEquals(intersectionNode, findClosestJunction(node00));
		assertEquals(intersectionNode, findClosestJunction(node01));
		assertEquals(intersectionNode, findClosestJunction(node02));
		assertEquals(intersectionNode, findClosestJunction(node10));
		assertEquals(intersectionNode, findClosestJunction(node20));

	}

	@Test
	public void twoConnectedJunctions() throws Exception {

		TestMapDataGenerator generator = new TestMapDataGenerator();

		/* prepare fake map data */

		MapNode intersectionNodeA = generator.createNode(0, 0);
		MapNode node01 = generator.createNode(1, 1);
		MapNode node02 = generator.createNode(2, 2);
		MapNode intersectionNodeB = generator.createNode(3, 3);

		MapNode node10 = generator.createNode(0, 1);
		MapNode node20 = generator.createNode(-1, 0);
		MapNode node30 = generator.createNode(3, 1);
		MapNode node40 = generator.createNode(4, 3);

		List<List<MapNode>> wayNodeLists = asList(
				asList(intersectionNodeA, node01, node02, intersectionNodeB),
				asList(intersectionNodeA, node10),
				asList(node20, intersectionNodeA),
				asList(intersectionNodeB, node30),
				asList(node40, intersectionNodeB));

		/* create ways and roads objects */

		for (List<MapNode> wayNodes : wayNodeLists) {
			MapWay way = generator.createWay(wayNodes, TagSet.of("highway", "tertiary"));
			way.getWaySegments().forEach(s -> s.addRepresentation(new Road(s)));
		}

		/* check that the correct junction node is found for each starting node */

		assertEquals(intersectionNodeA, findClosestJunction(node01));
		assertEquals(intersectionNodeA, findClosestJunction(node10));
		assertEquals(intersectionNodeA, findClosestJunction(node20));

		assertEquals(intersectionNodeB, findClosestJunction(node02));
		assertEquals(intersectionNodeB, findClosestJunction(node30));
		assertEquals(intersectionNodeB, findClosestJunction(node40));

	}

}
