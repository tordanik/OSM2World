package org.osm2world.math.geo;

import static org.junit.Assert.assertEquals;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.math.VectorXZ;

public class OrthographicAzimuthalMapProjectionTest extends AbstractMapProjectionTest {

	@Override
	protected MapProjection createProjection(LatLon origin) {
		return new OrthographicAzimuthalMapProjection(origin);
	}

	@Test
	public void testToXZ() {

		MapProjection proj = createProjection(new LatLon(0, 0));

		VectorXZ posOrigin = proj.toXZ(0, 0);
		assertAlmostEquals(0, 0, posOrigin);
		assertEquals(0, proj.toLat(posOrigin), DELTA);
		assertEquals(0, proj.toLon(posOrigin), DELTA);

		VectorXZ posE = proj.toXZ(0, 0.000009);
		assertAlmostEquals(1, 0, posE);
		assertEquals(0, proj.toLat(posE), DELTA);
		assertEquals(0.000009, proj.toLon(posE), DELTA);

		VectorXZ posN = proj.toXZ(0.000009, 0);
		assertAlmostEquals(0, 1, posN);
		assertEquals(0.000009, proj.toLat(posN), DELTA);
		assertEquals(0, proj.toLon(posN), DELTA);

	}

}
