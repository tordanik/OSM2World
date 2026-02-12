package org.osm2world.math.shapes;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.osm2world.math.VectorXZ.Z_UNIT;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.math.VectorXZ;

public class PolylineXZTest {

	PolylineXZ p1 = new PolylineXZ(asList(
			new VectorXZ(-1, 0),
			new VectorXZ( 0, 0),
			new VectorXZ( 1, 0),
			new VectorXZ( 1, 3),
			new VectorXZ(-1, 3)
			));

	@Test
	public void testGetLength() {

		assertEquals(7, p1.getLength(), 1e-5);

	}

	@Test
	public void testOffsetOf() {

		/* vertices of the polyline */

		assertEquals(0, p1.offsetOf(new VectorXZ(-1, 0)), 1e-5);
		assertEquals(5, p1.offsetOf(new VectorXZ( 1, 3)), 1e-5);
		assertEquals(7, p1.offsetOf(new VectorXZ(-1, 3)), 1e-5);

		/* points on the vertices of the polyline */

		assertEquals(0.5, p1.offsetOf(new VectorXZ(-0.5, 0)), 1e-5);
		assertEquals(1.5, p1.offsetOf(new VectorXZ(+0.5, 0)), 1e-5);
		assertEquals(3.0, p1.offsetOf(new VectorXZ(1, 1)), 1e-5);

	}

	@Test
	public void testPointAtOffset() {

		assertAlmostEquals(-1, 0, p1.pointAtOffset(0));
		assertAlmostEquals( 1, 3, p1.pointAtOffset(5));
		assertAlmostEquals(-1, 3, p1.pointAtOffset(7));

		assertAlmostEquals(-0.5, 0, p1.pointAtOffset(0.5));
		assertAlmostEquals(+0.5, 0, p1.pointAtOffset(1.5));
		assertAlmostEquals(1, 1, p1.pointAtOffset(3.0));

	}

	/**
	 * tests that {@link PolylineXZ#offsetOf(VectorXZ)}
	 * is the inverse of {@link PolylineXZ#pointAtOffset(double)}
	 */
	@Test
	public void testOffsetSymmetry() {

		for (int i = 0; i <= p1.getLength() * 10; i++) {

			double offset = i * 0.1;

			assertEquals(offset, p1.offsetOf(p1.pointAtOffset(offset)), 1e-5);

		}

	}

	@Test
	public void testClosestPoint() {

		VectorXZ v0 = new VectorXZ(-10, 2);
		VectorXZ v1 = new VectorXZ(  0, 2);
		VectorXZ v2 = new VectorXZ(+10, 2);

		PolylineXZ polyline = new PolylineXZ(asList(v0, v1, v2));

		for (VectorXZ v : polyline.vertices()) {
			assertAlmostEquals(v, polyline.closestPoint(v));
		}

		assertAlmostEquals(5, 2, polyline.closestPoint(new VectorXZ(5, 5)));
		assertAlmostEquals(-3, 2, polyline.closestPoint(new VectorXZ(-3, 2)));

		assertAlmostEquals(v2, polyline.closestPoint(new VectorXZ(15, 0)));

	}


	@Test
	public void testClosestSegment() {

		VectorXZ v0 = new VectorXZ(-10, 2);
		VectorXZ v1 = new VectorXZ(  0, 2);
		VectorXZ v2 = new VectorXZ(+10, 2);

		PolylineXZ polyline = new PolylineXZ(asList(v0, v1, v2));

		for (LineSegmentXZ s : polyline.getSegments()) {
			assertEquals(s, polyline.closestSegment(s.getCenter()));
		}

		assertEquals(polyline.getSegments().get(0), polyline.closestSegment(new VectorXZ(-5, 4)));

	}

	@Test
	public void testEquals() {

		PolylineXZ polyline1 = new PolylineXZ(new VectorXZ(0, 0), new VectorXZ(1, 0));
		PolylineXZ polyline2 = new PolylineXZ(new VectorXZ(0, 1), new VectorXZ(1, 1));

		assertEquals(polyline1, polyline1);
		assertEquals(polyline2, polyline2);
		assertNotEquals(polyline1, polyline2);

		assertEquals(polyline1, new PolylineXZ(polyline1.vertices()));
		assertEquals(polyline1, polyline1.reverse().reverse());
		assertNotEquals(polyline1, polyline1.reverse());

		assertEquals(polyline1.shift(Z_UNIT), polyline1.shift(Z_UNIT));
		assertEquals(polyline2, polyline1.shift(Z_UNIT));

	}

}
