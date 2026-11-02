package org.osm2world;

import static java.lang.Math.sqrt;
import static org.junit.Assert.*;
import static org.osm2world.math.VectorXYZ.Y_UNIT;
import static org.osm2world.math.VectorXYZ.Z_UNIT;
import static org.osm2world.math.algorithms.GeometryUtil.closeLoop;
import static org.osm2world.test.TestUtil.assertAlmostEquals;
import static org.osm2world.world.connectors.Connector.AttachmentProperties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.shapes.FaceXYZ;
import org.osm2world.osm.creation.OSMFileReader;
import org.osm2world.util.test.TestFileUtil;
import org.osm2world.world.connectors.AttachmentSurface;
import org.osm2world.world.connectors.Connector;
import org.osm2world.world.connectors.GroundState;
import org.osm2world.world.modules.BridgeModule;
import org.osm2world.world.modules.RoadModule;
import org.osm2world.world.modules.StreetFurnitureModule;
import org.osm2world.world.modules.building.Building;
import org.osm2world.world.modules.traffic_sign.TrafficSignGroup;

public class O2WConverterImplTest {

	@Test
	public void testAttachConnectorToHorizontalSurface() {

		AttachmentSurface surface = new AttachmentSurface(List.of("test", "other1"), null, List.of(new FaceXYZ(closeLoop(
				new VectorXYZ(-1, 5, 0),
				new VectorXYZ(+1, 5, 0),
				new VectorXYZ(0, 5, 2)))));

		var connector = new Connector(new VectorXYZ(0, 2, 1), null, GroundState.ATTACHED,
				new AttachmentProperties(List.of("test", "other2"),0, false), null);

		O2WConverterImpl.attachConnectorIfValid(connector, surface);

		assertTrue(connector.isAttached());
		assertSame(surface, connector.getAttachedSurface());
		assertAlmostEquals(Y_UNIT, connector.getAttachedSurfaceNormal());
		assertAlmostEquals(0, 5, 1, connector.getPosXYZ());

	}

	@Test
	public void testAttachConnectorToVerticalSurface() {

		AttachmentSurface surface = new AttachmentSurface(List.of("test"), null, List.of(new FaceXYZ(closeLoop(
				new VectorXYZ(-10, 0, 0),
				new VectorXYZ(+10, 0, 0),
				new VectorXYZ(0, 20, 0)))));

		for (double preferredHeight : List.of(10, 15)) {

			var connector = new Connector(new VectorXYZ(0, 10, 1), null, GroundState.ATTACHED,
					new AttachmentProperties(List.of("test"), preferredHeight, true), null);

			O2WConverterImpl.attachConnectorIfValid(connector, surface);

			assertTrue(connector.isAttached());
			assertSame(surface, connector.getAttachedSurface());
			assertAlmostEquals(Z_UNIT.invert(), connector.getAttachedSurfaceNormal());
			assertAlmostEquals(0, preferredHeight, 0, connector.getPosXYZ());

		}

	}

	@Test
	public void testAttachConnectorToDiagonalSurface() {

		AttachmentSurface surface = new AttachmentSurface(List.of("test"), null, List.of(new FaceXYZ(closeLoop(
				new VectorXYZ(-10, 0, 0),
				new VectorXYZ(+10, 0, 0),
				new VectorXYZ(0, 20, 20)))));

		for (boolean changeXZ : List.of(true, false)) {

			var connector = new Connector(new VectorXYZ(0, 20, 10), null, GroundState.ATTACHED,
					new AttachmentProperties(List.of("test"), 20, changeXZ), null);

			O2WConverterImpl.attachConnectorIfValid(connector, surface);

			assertTrue(connector.isAttached());
			assertSame(surface, connector.getAttachedSurface());
			assertAlmostEquals(0, sqrt(2)/2, -sqrt(2)/2, connector.getAttachedSurfaceNormal());

			if (changeXZ) {
				assertAlmostEquals(0, 15, 15, connector.getPosXYZ());
			} else {
				assertAlmostEquals(0, 10, 10, connector.getPosXYZ());
			}

		}

	}

	@Test
	public void testRecursiveAttachment() throws IOException {

		File osmFile = TestFileUtil.getTestFile("recursiveAttachmentTest.osm");

		var o2w = new O2WConverterImpl(null, List.of());
		var scene = o2w.convert(new OSMFileReader(osmFile), null, null);

		var trafficSigns = scene.getWorldObjects(TrafficSignGroup.class);

		assertTrue(trafficSigns.iterator().hasNext());

		var trafficSign = trafficSigns.iterator().next();

		double minY = trafficSign.buildMeshesForModelHierarchy().stream()
				.flatMap(m -> m.geometry.asTriangles().triangles.stream())
				.flatMap(t -> t.verticesNoDup().stream())
				.mapToDouble(v -> v.y)
				.min().orElseThrow();

		assertTrue("minY is " + minY + ", needs to be > 10", minY > 10);

	}

	@Test
	public void testBridgeBetweenBuildings() throws IOException {

		for (String fileName : List.of("building_bridge_test.osm", "building_bridge_test_polygon.osm",
				"building_bridge_test_buildingpart.osm")) {

			var o2w = new O2WConverterImpl(null, List.of());
			var scene = o2w.convert(new OSMFileReader(TestFileUtil.getTestFile(fileName)), null, null);

			List<Connector> entranceConnectors = new ArrayList<>();
			scene.getWorldObjects(Building.class).forEach(b -> b.getConnectors().stream()
					.filter(c -> c.reference instanceof MapNode n && n.getTags().containsKey("entrance"))
					.forEach(entranceConnectors::add));

			List<BridgeModule.Bridge<?>> bridges = new ArrayList<>();
			scene.getWorldObjects(BridgeModule.Bridge.class).forEach(bridges::add);
			List<Connector> bridgeConnectors = new ArrayList<>();
			bridges.forEach(b -> b.getConnectors().forEach(bridgeConnectors::add));

			assertEquals(fileName, 2, entranceConnectors.size());

			// the buildings stay on the ground (no terrain data, so the ground is at 0),
			// they are not moved by the bridge or by objects attached to the bridge
			scene.getWorldObjects(Building.class).forEach(b ->
					assertEquals(fileName, 0, b.getGroundLevelEle(), 1e-6));

			for (Connector entrance : entranceConnectors) {

				// the entrances are on upper levels
				assertTrue(fileName, entrance.getPosXYZ().y > 2);

				// the bridge's ends are at the height of the entrances
				List<Connector> connectedBridgeConnectors = bridgeConnectors.stream()
						.filter(c -> c.reference == entrance.reference)
						.toList();
				assertFalse(fileName, connectedBridgeConnectors.isEmpty());
				for (Connector c : connectedBridgeConnectors) {
					assertEquals(fileName, entrance.getPosXYZ().y, c.getPosXYZ().y, 1e-6);
				}

			}

			// the street lamp is attached to the bridge, between the two entrances
			scene.getWorldObjects(StreetFurnitureModule.StreetLamp.class).forEach(lamp -> {
				Connector c = lamp.getConnectors().iterator().next();
				assertTrue(fileName, c.isAttached());
				assertTrue(fileName, c.getPosXYZ().y > 2);
			});

		}

	}

	@Test
	public void testRoadBetweenEntrances() throws IOException {

		var builder = new MapDataBuilder();
		TagSet entranceTags = TagSet.of("entrance", "yes", "level", "1");
		TagSet buildingTags = TagSet.of("building", "yes", "building:levels", "3");

		MapNode entranceA = builder.createNode(10, 5, entranceTags);
		builder.createWayArea(List.of(builder.createNode(0, 0), builder.createNode(10, 0), entranceA,
				builder.createNode(10, 10), builder.createNode(0, 10)), buildingTags);

		MapNode entranceB = builder.createNode(20, 5, entranceTags);
		builder.createWayArea(List.of(builder.createNode(20, 0), builder.createNode(30, 0),
				builder.createNode(30, 10), builder.createNode(20, 10), entranceB), buildingTags);

		builder.createWay(List.of(entranceA, entranceB), TagSet.of("highway", "footway"));

		var scene = new O2WTestConverter().convert(builder.build(), null);

		List<Connector> entranceConnectors = new ArrayList<>();
		scene.getWorldObjects(Building.class).forEach(b -> b.getConnectors().stream()
				.filter(c -> c.reference instanceof MapNode)
				.forEach(entranceConnectors::add));
		assertEquals(2, entranceConnectors.size());
		double entranceEle = entranceConnectors.get(0).getPosXYZ().y;
		assertTrue(entranceEle > 2);

		// both ends of the footway are at the height of the entrances
		var road = scene.getWorldObjects(RoadModule.Road.class).iterator().next();
		List<VectorXYZ> centerline = road.getCenterline();
		assertEquals(entranceEle, centerline.get(0).y, 1e-6);
		assertEquals(entranceEle, centerline.get(centerline.size() - 1).y, 1e-6);

	}

}
