package org.osm2world;

import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.osm2world.math.VectorXYZ.Y_UNIT;
import static org.osm2world.math.VectorXYZ.Z_UNIT;
import static org.osm2world.math.algorithms.GeometryUtil.closeLoop;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.shapes.FaceXYZ;
import org.osm2world.world.attachment.AttachmentConnector;
import org.osm2world.world.attachment.AttachmentSurface;

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

}
