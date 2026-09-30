package org.osm2world.world.modules;

import static java.lang.Math.sqrt;
import static org.junit.Assert.*;
import static org.osm2world.test.TestUtil.assertAlmostEquals;
import static org.osm2world.util.ListUtil.getFirst;
import static org.osm2world.util.ListUtil.getLast;
import static org.osm2world.world.modules.BridgeModule.Bridge.calculateCenterlineBetween;
import static org.osm2world.world.modules.BridgeModule.Bridge.circularArcHeightAt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osm2world.O2WTestConverter;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapWay;
import org.osm2world.map_data.data.MapWaySegment;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolylineXZ;
import org.osm2world.scene.Scene;
import org.osm2world.world.modules.BridgeModule.Bridge;

public class BridgeModuleTest {

	@Test
	public void testCreateCenterlineBetween_straightEdges() {

		var edge0 = new LineSegmentXZ(new VectorXZ(-5, 0), new VectorXZ(-5, 10));
		var edge1A = new LineSegmentXZ(new VectorXZ(3, 0), new VectorXZ(3, 10));

		PolylineXZ resultA = calculateCenterlineBetween(edge0, edge1A);

		assertNotNull(resultA);
		List<VectorXZ> expected = List.of(new VectorXZ(-1, 0), new VectorXZ(-1, 10));
		assertAlmostEquals(expected, resultA.vertices());

		var edge1B = new PolylineXZ(new VectorXZ(3, 0), new VectorXZ(3, 2), new VectorXZ(3, 10));
		PolylineXZ resultB = calculateCenterlineBetween(edge0, edge1B);

		assertNotNull(resultB);
		assertAlmostEquals(expected.get(0), getFirst(resultB.vertices()));
		assertAlmostEquals(expected.get(1), getLast(resultB.vertices()));
		assertEquals(10.0, resultB.getLength(), 0.01);

	}

	@Test
	public void testCreateCenterlineBetween_corner() {

		var edge0 = new PolylineXZ(new VectorXZ(0, 0), new VectorXZ(0, 5), new VectorXZ(-5, 5));
		var edge1 = new PolylineXZ(new VectorXZ(5, 0), new VectorXZ(5, 10), new VectorXZ(-5, 10));

		PolylineXZ result = calculateCenterlineBetween(edge0, edge1);

		assertNotNull(result);
		assertEquals(15, result.getLength(), 0.1);

	}

	@Test
	public void testCircularArcHeightAt() {

		assertEquals(0, circularArcHeightAt(10, 0, 1), 0.01);
		assertEquals(sqrt(75), circularArcHeightAt(10, 0.25, 1), 0.01);
		assertEquals(10, circularArcHeightAt(10, 0.5, 1), 0.01);
		assertEquals(sqrt(75), circularArcHeightAt(10, 0.75, 1), 0.01);
		assertEquals(0, circularArcHeightAt(10, 1.0, 1), 0.01);

		assertEquals(0, circularArcHeightAt(10, 0, 0.5), 0.01);
		assertEquals(10, circularArcHeightAt(10, 0.5, 0.5), 0.01);
		assertEquals(0, circularArcHeightAt(10, 1, 0.5), 0.01);

	}

	@Test
	public void testConnectedWaysFormSingleBridge() throws IOException {

		var builder = new MapDataBuilder();
		MapNode n0 = builder.createNode(0, 0);
		MapNode n1 = builder.createNode(20, 0);
		MapNode n2 = builder.createNode(40, 5);
		MapNode n3 = builder.createNode(60, 5);

		TagSet tags = TagSet.of("highway", "residential", "bridge", "yes");
		MapWay wayA = builder.createWay(List.of(n0, n1, n2), tags);
		MapWay wayB = builder.createWay(List.of(n3, n2), tags); // opposite direction

		List<Bridge<?>> bridges = convertAndGetBridges(builder);

		assertEquals(1, bridges.size());
		Bridge<?> bridge = bridges.get(0);

		List<MapWaySegment> segments = new ArrayList<>(wayA.getWaySegments());
		segments.addAll(wayB.getWaySegments());
		for (MapWaySegment segment : segments) {
			assertTrue(segment.getRepresentations().contains(bridge));
		}

		assertEquals(20 + n1.getPos().distanceTo(n2.getPos()) + 20, bridge.getCenterline().getLength(), 0.1);
		assertEquals(2, bridge.getCaps().size());

	}

	@Test
	public void testBranchingWaySplitsBridge() throws IOException {

		var builder = new MapDataBuilder();
		MapNode n0 = builder.createNode(0, 0);
		MapNode n1 = builder.createNode(20, 0);
		MapNode n2 = builder.createNode(40, 0);
		MapNode n3 = builder.createNode(60, 0);
		MapNode n4 = builder.createNode(20, 20);

		builder.createWay(List.of(n0, n1, n2, n3), TagSet.of("highway", "residential", "bridge", "yes"));
		builder.createWay(List.of(n1, n4), TagSet.of("highway", "service"));

		assertEquals(2, convertAndGetBridges(builder).size());

	}

	@Test
	public void testDifferentBridgePropertiesSplitBridge() throws IOException {

		List<TagSet> differentTags = List.of(
				TagSet.of("highway", "residential", "bridge", "viaduct"),
				TagSet.of("highway", "residential", "bridge", "yes", "bridge:structure", "arch"),
				TagSet.of("highway", "residential", "bridge", "yes", "layer", "2"));

		for (TagSet tagsB : differentTags) {

			var builder = new MapDataBuilder();
			MapNode n0 = builder.createNode(0, 0);
			MapNode n1 = builder.createNode(20, 0);
			MapNode n2 = builder.createNode(40, 0);
			MapNode n3 = builder.createNode(60, 0);

			TagSet tagsA = TagSet.of("highway", "residential", "bridge", "yes", "layer", "1");
			builder.createWay(List.of(n0, n1), tagsA);
			builder.createWay(List.of(n1, n2), tagsA);
			builder.createWay(List.of(n2, n3), tagsB);

			assertEquals(tagsB.toString(), 2, convertAndGetBridges(builder).size());

		}

	}

	@Test
	public void testBridgeRelationMembershipSplitsBridge() throws IOException {

		var builder = new MapDataBuilder();
		MapNode n0 = builder.createNode(0, 0);
		MapNode n1 = builder.createNode(20, 0);
		MapNode n2 = builder.createNode(40, 0);
		MapNode n3 = builder.createNode(60, 0);

		TagSet tags = TagSet.of("highway", "residential", "bridge", "yes");
		MapWay wayA = builder.createWay(List.of(n0, n1), tags);
		MapWay wayB = builder.createWay(List.of(n1, n2), tags);
		builder.createWay(List.of(n2, n3), tags);

		builder.createRelation(List.of(Map.entry("across", wayA), Map.entry("across", wayB)),
				TagSet.of("type", "bridge"));

		assertEquals(2, convertAndGetBridges(builder).size());

	}

	private static List<Bridge<?>> convertAndGetBridges(MapDataBuilder builder) throws IOException {
		Scene scene = new O2WTestConverter().convert(builder.build(), null);
		List<Bridge<?>> result = new ArrayList<>();
		scene.getWorldObjects(Bridge.class).forEach(result::add);
		return result;
	}


}
