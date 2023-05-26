package org.osm2world.core;

import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.osm2world.core.math.GeometryUtil.closeLoop;
import static org.osm2world.core.math.VectorXYZ.Y_UNIT;
import static org.osm2world.core.math.VectorXYZ.Z_UNIT;
import static org.osm2world.core.target.gltf.GltfTarget.GltfFlavor.GLTF;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import java.nio.file.Files;
import java.util.List;

import org.junit.Test;
import org.osm2world.core.map_data.creation.LatLon;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_data.creation.MetricMapProjection;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.FaceXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.gltf.GltfTarget;
import org.osm2world.core.test.TestMapDataGenerator;
import org.osm2world.core.world.attachment.AttachmentConnector;
import org.osm2world.core.world.attachment.AttachmentSurface;

public class ConversionFacadeTest {

	@Test
	public void testAttachConnectorToHorizontalSurface() {

		AttachmentSurface surface = new AttachmentSurface(asList("test", "other1"), asList(new FaceXYZ(closeLoop(
				new VectorXYZ(-1, 5, 0),
				new VectorXYZ(+1, 5, 0),
				new VectorXYZ( 0, 5, 2)))));

		AttachmentConnector connector = new AttachmentConnector(asList("test", "other2"),
				new VectorXYZ(0, 2, 1),
				null, 0, false);

		ConversionFacade.attachConnectorIfValid(connector, surface);

		assertTrue(connector.isAttached());
		assertSame(surface, connector.getAttachedSurface());
		assertAlmostEquals(Y_UNIT, connector.getAttachedSurfaceNormal());
		assertAlmostEquals(0, 5, 1, connector.getAttachedPos());

	}

	@Test
	public void testAttachConnectorToVerticalSurface() {

		AttachmentSurface surface = new AttachmentSurface(asList("test"), asList(new FaceXYZ(closeLoop(
				new VectorXYZ(-10, 0, 0),
				new VectorXYZ(+10, 0, 0),
				new VectorXYZ(0, 20, 0)))));

		for (double preferredHeight : asList(10, 15)) {

			AttachmentConnector connector = new AttachmentConnector(asList("test"),
					new VectorXYZ(0, 10, 1),
					null, preferredHeight, true);

			ConversionFacade.attachConnectorIfValid(connector, surface);

			assertTrue(connector.isAttached());
			assertSame(surface, connector.getAttachedSurface());
			assertAlmostEquals(Z_UNIT.invert(), connector.getAttachedSurfaceNormal());
			assertAlmostEquals(0, preferredHeight, 0, connector.getAttachedPos());

		}

	}

	@Test
	public void testAttachConnectorToDiagonalSurface() {

		AttachmentSurface surface = new AttachmentSurface(asList("test"), asList(new FaceXYZ(closeLoop(
				new VectorXYZ(-10, 0, 0),
				new VectorXYZ(+10, 0, 0),
				new VectorXYZ(0, 20, 20)))));

		for (boolean changeXZ : asList(true, false)) {

			AttachmentConnector connector = new AttachmentConnector(asList("test"),
					new VectorXYZ(0, 20, 10),
					null, 20, changeXZ);

			ConversionFacade.attachConnectorIfValid(connector, surface);

			assertTrue(connector.isAttached());
			assertSame(surface, connector.getAttachedSurface());
			assertAlmostEquals(0, sqrt(2)/2, -sqrt(2)/2, connector.getAttachedSurfaceNormal());

			if (changeXZ) {
				assertAlmostEquals(0, 15, 15, connector.getAttachedPos());
			} else {
				assertAlmostEquals(0, 10, 10, connector.getAttachedPos());
			}

		}

	}

	@Test
	public void testAreasWithDuplicateNodes() {

		List<TagSet> tagSets = asList(TagSet.of("building", "yes"));

		for (TagSet tagSet : tagSets) {

			TestMapDataGenerator generator = new TestMapDataGenerator();

			List<MapNode> nodes = asList(
					generator.createNode(0, 0),
					generator.createNode(10, 0),
					generator.createNode(10, 5),
					generator.createNode(10, 5),
					generator.createNode(10, 10),
					generator.createNode(0, 10));
			generator.createWayArea(nodes, tagSet);

			MapData mapData = generator.createMapData();

			try {

				Target testTarget = new GltfTarget(Files.createTempFile("o2w-test-", ".gltf").toFile(), GLTF, null);
				MapProjection mapProjection = new MetricMapProjection(new LatLon(0, 0));
				ConversionFacade cf = new ConversionFacade();

				cf.createRepresentations(mapProjection, mapData, null, null, asList(testTarget));

			} catch (Exception e) {
				throw new AssertionError("Conversion failed for tags: " +  tagSet, e);
			}

		}

	}

}
