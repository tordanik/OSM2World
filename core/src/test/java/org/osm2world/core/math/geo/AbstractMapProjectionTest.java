package org.osm2world.core.math.geo;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.osm2world.core.math.VectorXZ;

public abstract class AbstractMapProjectionTest {

	abstract protected MapProjection createProjection(LatLon origin);

	/** precision expected for {@link LatLon} results; different from that for XYZ coords */
	protected static final double DELTA = 1e-6;

	@Test
	public void testOriginAndAxes() {

		List<LatLon> origins = asList(new LatLon(0, 0), new LatLon(80, -170), new LatLon(-55, 33));

		for (LatLon origin : origins) {

			MapProjection proj = createProjection(origin);

			assertAlmostEquals(0, 0, proj.toXZ(origin));
			assertEquals(origin.lat, proj.toLat(new VectorXZ(0, 0)), DELTA);
			assertEquals(origin.lon, proj.toLon(new VectorXZ(0, 0)), DELTA);

			VectorXZ northPoint = proj.toXZ(origin).add(0, 1);
			assertTrue(origin.lat < proj.toLat(northPoint));
			assertEquals(origin.lon, proj.toLon(northPoint), DELTA);

			VectorXZ eastPoint = proj.toXZ(origin).add(1, 0);
			assertEquals(origin.lat, proj.toLat(eastPoint), DELTA);
			assertTrue(origin.lon < proj.toLon(eastPoint));

		}

	}

	@Ignore //TODO: Projections (and LatLon in general) are likely to not work properly across the date boundary
	@Test
	public void testDateBoundary() {

		MapProjection proj = createProjection(new LatLon(0, 179.999999));

		VectorXZ pointAcrossBoundary = proj.toXZ(new LatLon(0, -179.999999));
		assertEquals(0, pointAcrossBoundary.z, DELTA);
		assertTrue("point was: " + pointAcrossBoundary, pointAcrossBoundary.x > 0);

	}

}
