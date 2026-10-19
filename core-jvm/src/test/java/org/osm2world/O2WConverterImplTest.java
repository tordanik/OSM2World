package org.osm2world;

import static java.lang.Math.sqrt;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.osm2world.map_elevation.data.EleConnector.AttachmentProperties;
import static org.osm2world.math.VectorXYZ.Y_UNIT;
import static org.osm2world.math.VectorXYZ.Z_UNIT;
import static org.osm2world.math.algorithms.GeometryUtil.closeLoop;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import java.util.List;

import org.junit.Test;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.shapes.FaceXYZ;
import org.osm2world.world.attachment.AttachmentSurface;

public class O2WConverterImplTest {

	@Test
	public void testAttachConnectorToHorizontalSurface() {

		AttachmentSurface surface = new AttachmentSurface(List.of("test", "other1"), null, List.of(new FaceXYZ(closeLoop(
				new VectorXYZ(-1, 5, 0),
				new VectorXYZ(+1, 5, 0),
				new VectorXYZ(0, 5, 2)))));

		var connector = new EleConnector(new VectorXYZ(0, 2, 1), null, GroundState.ATTACHED,
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

			var connector = new EleConnector(new VectorXYZ(0, 10, 1), null, GroundState.ATTACHED,
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

			var connector = new EleConnector(new VectorXYZ(0, 20, 10), null, GroundState.ATTACHED,
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

}
