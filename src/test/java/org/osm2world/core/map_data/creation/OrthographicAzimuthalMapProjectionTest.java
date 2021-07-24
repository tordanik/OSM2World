package org.osm2world.core.map_data.creation;

import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.core.math.VectorXZ;


public class OrthographicAzimuthalMapProjectionTest {

	@Test
	public void testCalcPos() {

		OrthographicAzimuthalMapProjection proj = new OrthographicAzimuthalMapProjection(new LatLon(0, 0));

		VectorXZ posOrigin = proj.toXZ(0, 0);
		assertAlmostEquals(0, 0, posOrigin);
		assertAlmostEquals(0, proj.toLat(posOrigin));
		assertAlmostEquals(0, proj.toLon(posOrigin));

		VectorXZ posE = proj.toXZ(0, 0.000009);
		assertAlmostEquals(1, 0, posE);
		assertAlmostEquals(0, proj.toLat(posE));
		assertAlmostEquals(0.000009, proj.toLon(posE));

		VectorXZ posN = proj.toXZ(0.000009, 0);
		assertAlmostEquals(0, 1, posN);
		assertAlmostEquals(0.000009, proj.toLat(posN));
		assertAlmostEquals(0, proj.toLon(posN));

	}

}
