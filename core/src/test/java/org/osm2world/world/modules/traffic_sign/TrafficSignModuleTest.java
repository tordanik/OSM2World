package org.osm2world.world.modules.traffic_sign;

import static java.lang.Math.PI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.osm2world.world.modules.traffic_sign.TrafficSignModule.findClosestJunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.osm2world.O2WTestConverter;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapWay;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.world.modules.RoadModule.Road;

public class TrafficSignModuleTest {

	@Test
	public void testSignFromHighwayNode() throws IOException {

		List<TagSet> inputs = List.of(
				TagSet.of("traffic_sign", "DE:240", "direction", "backward"),
				TagSet.of("traffic_sign", "DE:240", "traffic_sign:direction", "backward"),
				TagSet.of("traffic_sign:backward", "DE:240"),
				TagSet.of("traffic_sign", "DE:240", "direction", "forward"),
				TagSet.of("traffic_sign", "DE:240", "traffic_sign:direction", "forward"),
				TagSet.of("traffic_sign:forward", "DE:240"),
				TagSet.of("traffic_sign", "DE:240", "direction", "NW"),
				TagSet.of("traffic_sign", "DE:240", "direction", "backward", "side", "both")

		);

		List<List<TrafficSignGroup>> results = new ArrayList<>();

		for (TagSet signTags : inputs) {

			/* prepare fake map data */

			var builder = new MapDataBuilder();

			MapNode nodeBefore = builder.createNode(0, 0);
			MapNode nodeSign = builder.createNode(1, 1, signTags);
			MapNode nodeAfter = builder.createNode(2, 2);

			builder.createWay(List.of(nodeBefore, nodeSign, nodeAfter), TagSet.of("highway", "tertiary"));

			/* generate models */

			var scene = new O2WTestConverter().convert(builder.build(), null);

			/* extract results */

			List<TrafficSignGroup> signs = new ArrayList<>();
			scene.getWorldObjects(TrafficSignGroup.class).forEach(signs::add);
			results.add(signs);

		}

		/* check the results */

		assertEquals(PI * 0.25, results.get(0).get(0).direction, 0.01);
		assertEquals(PI * 0.25, results.get(1).get(0).direction, 0.01);
		assertEquals(PI * 0.25, results.get(2).get(0).direction, 0.01);
		assertEquals(PI * 1.25, results.get(3).get(0).direction, 0.01);
		assertEquals(PI * 1.25, results.get(4).get(0).direction, 0.01);
		assertEquals(PI * 1.25, results.get(5).get(0).direction, 0.01);
		assertEquals(PI * 1.75, results.get(6).get(0).direction, 0.01);

		assertEquals(2, results.get(7).size());
		assertEquals(PI * 0.25, results.get(7).get(0).direction, 0.01);
		assertEquals(PI * 0.25, results.get(7).get(1).direction, 0.01);

	}

	@Test
	public void zeroConnectedJunctions() throws Exception {

		var builder = new MapDataBuilder();

		/* prepare fake map data */

		MapNode node00 = builder.createNode(0, 0);
		MapNode node01 = builder.createNode(1, 1);
		MapNode node02 = builder.createNode(2, 2);
		MapNode node03 = builder.createNode(3, 3);

		/* create a way and roads objects */

		List<MapNode> wayNodes = List.of(node00, node01, node02, node03);
		MapWay way = builder.createWay(wayNodes, TagSet.of("highway", "tertiary"));
		way.getWaySegments().forEach(s -> s.addRepresentation(new Road(s, new O2WConfig())));

		/* check that no junction is found for any starting node */

		assertNull(findClosestJunction(node00));
		assertNull(findClosestJunction(node01));
		assertNull(findClosestJunction(node02));
		assertNull(findClosestJunction(node03));

	}

	@Test
	public void oneConnectedJunction() throws Exception {

		var builder = new MapDataBuilder();

		/* prepare fake map data */

		MapNode node00 = builder.createNode(0, 0);
		MapNode node01 = builder.createNode(1, 1);
		MapNode node02 = builder.createNode(2, 2);
		MapNode intersectionNode = builder.createNode(3, 3);

		MapNode node10 = builder.createNode(3, 1);
		MapNode node20 = builder.createNode(4, 3);

		List<List<MapNode>> wayNodeLists = List.of(
				List.of(node00, node01, node02, intersectionNode),
				List.of(intersectionNode, node10),
				List.of(intersectionNode, node20));

		/* create ways and roads objects */

		for (List<MapNode> wayNodes : wayNodeLists) {
			MapWay way = builder.createWay(wayNodes, TagSet.of("highway", "tertiary"));
			way.getWaySegments().forEach(s -> s.addRepresentation(new Road(s, new O2WConfig())));
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

		var builder = new MapDataBuilder();

		/* prepare fake map data */

		MapNode intersectionNodeA = builder.createNode(0, 0);
		MapNode node01 = builder.createNode(1, 1);
		MapNode node02 = builder.createNode(2, 2);
		MapNode intersectionNodeB = builder.createNode(3, 3);

		MapNode node10 = builder.createNode(0, 1);
		MapNode node20 = builder.createNode(-1, 0);
		MapNode node30 = builder.createNode(3, 1);
		MapNode node40 = builder.createNode(4, 3);

		List<List<MapNode>> wayNodeLists = List.of(
				List.of(intersectionNodeA, node01, node02, intersectionNodeB),
				List.of(intersectionNodeA, node10),
				List.of(node20, intersectionNodeA),
				List.of(intersectionNodeB, node30),
				List.of(node40, intersectionNodeB));

		/* create ways and roads objects */

		for (List<MapNode> wayNodes : wayNodeLists) {
			MapWay way = builder.createWay(wayNodes, TagSet.of("highway", "tertiary"));
			way.getWaySegments().forEach(s -> s.addRepresentation(new Road(s, new O2WConfig())));
		}

		/* check that the correct junction node is found for each starting node */

		assertEquals(intersectionNodeA, findClosestJunction(node01));
		assertEquals(intersectionNodeA, findClosestJunction(node10));
		assertEquals(intersectionNodeA, findClosestJunction(node20));

		assertEquals(intersectionNodeB, findClosestJunction(node02));
		assertEquals(intersectionNodeB, findClosestJunction(node30));
		assertEquals(intersectionNodeB, findClosestJunction(node40));

	}

	@Test
	public void zeroConnectedJunctionsWithLoop() {

		var builder = new MapDataBuilder();

		/* prepare fake map data */

		MapNode node00 = builder.createNode(0, 0);
		MapNode node01 = builder.createNode(1, 1);
		MapNode node02 = builder.createNode(2, 2);
		MapNode node03 = builder.createNode(3, 0);

		/* create a way and roads objects */

		List<MapNode> wayNodes = List.of(node00, node01, node02, node03, node00);
		MapWay way = builder.createWay(wayNodes, TagSet.of("highway", "tertiary"));
		way.getWaySegments().forEach(s -> s.addRepresentation(new Road(s, new O2WConfig())));

		/* check that there is no infinite loop and that no junction is reported */

		assertNull(findClosestJunction(node00));
		assertNull(findClosestJunction(node01));
		assertNull(findClosestJunction(node02));
		assertNull(findClosestJunction(node03));

	}

}
