package org.osm2world.core.world.modules.traffic_sign;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.osm2world.core.world.modules.traffic_sign.TrafficSignModule.findClosestJunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.MapConfiguration;
import org.junit.Test;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.map_data.creation.LatLon;
import org.osm2world.core.map_data.creation.MetricMapProjection;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.test.TestMapDataGenerator;
import org.osm2world.core.world.modules.RoadModule.Road;

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

			TestMapDataGenerator generator = new TestMapDataGenerator();

			MapNode nodeBefore = generator.createNode(0, 0);
			MapNode nodeSign = generator.createNode(1, 1, signTags);
			MapNode nodeAfter = generator.createNode(2, 2);

			generator.createWay(List.of(nodeBefore, nodeSign, nodeAfter), TagSet.of("highway", "tertiary"));

			/* generate models */

			var proj = new MetricMapProjection(new LatLon(0, 0));
			var result = new ConversionFacade().createRepresentations(
					proj, generator.createMapData(), null, null, null);

			/* extract results */

			results.add(nodeSign.getRepresentations().stream()
					.filter(it -> it instanceof TrafficSignGroup)
					.map(TrafficSignGroup.class::cast)
					.toList());

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

		TestMapDataGenerator generator = new TestMapDataGenerator();

		/* prepare fake map data */

		MapNode node00 = generator.createNode(0, 0);
		MapNode node01 = generator.createNode(1, 1);
		MapNode node02 = generator.createNode(2, 2);
		MapNode node03 = generator.createNode(3, 3);

		/* create a way and roads objects */

		List<MapNode> wayNodes = asList(node00, node01, node02, node03);
		MapWay way = generator.createWay(wayNodes, TagSet.of("highway", "tertiary"));
		way.getWaySegments().forEach(s -> s.addRepresentation(new Road(s, new MapConfiguration(emptyMap()))));

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
			way.getWaySegments().forEach(s -> s.addRepresentation(new Road(s, new MapConfiguration(emptyMap()))));
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
			way.getWaySegments().forEach(s -> s.addRepresentation(new Road(s, new MapConfiguration(emptyMap()))));
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

		TestMapDataGenerator generator = new TestMapDataGenerator();

		/* prepare fake map data */

		MapNode node00 = generator.createNode(0, 0);
		MapNode node01 = generator.createNode(1, 1);
		MapNode node02 = generator.createNode(2, 2);
		MapNode node03 = generator.createNode(3, 0);

		/* create a way and roads objects */

		List<MapNode> wayNodes = asList(node00, node01, node02, node03, node00);
		MapWay way = generator.createWay(wayNodes, TagSet.of("highway", "tertiary"));
		way.getWaySegments().forEach(s -> s.addRepresentation(new Road(s, new MapConfiguration(emptyMap()))));

		/* check that there is no infinite loop and that no junction is reported */

		assertNull(findClosestJunction(node00));
		assertNull(findClosestJunction(node01));
		assertNull(findClosestJunction(node02));
		assertNull(findClosestJunction(node03));

	}

}
