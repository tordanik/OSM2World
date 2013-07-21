package org.osm2world.core.map_data.creation;

import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.core.math.VectorXZ;


public class OrthographicAzimuthalMapProjectionTest {

	@Test
	public void testCalcPos() {
		
		OrthographicAzimuthalMapProjection proj =
				new OrthographicAzimuthalMapProjection();
		proj.setOrigin(new LatLon(0, 0));
		
		VectorXZ posOrigin = proj.calcPos(0, 0);
		assertAlmostEquals(0, posOrigin.x);
		assertAlmostEquals(0, posOrigin.z);
		assertAlmostEquals(0, proj.calcLat(posOrigin));
		assertAlmostEquals(0, proj.calcLon(posOrigin));
		
		VectorXZ posE = proj.calcPos(0, 0.000009);
		assertAlmostEquals(1, posE.x);
		assertAlmostEquals(0, posE.z);
		assertAlmostEquals(0, proj.calcLat(posE));
		assertAlmostEquals(0.000009, proj.calcLon(posE));

		VectorXZ posN = proj.calcPos(0.000009, 0);
		assertAlmostEquals(0, posN.x);
		assertAlmostEquals(1, posN.z);
		assertAlmostEquals(0.000009, proj.calcLat(posN));
		assertAlmostEquals(0, proj.calcLon(posN));
		
	}
	
}
