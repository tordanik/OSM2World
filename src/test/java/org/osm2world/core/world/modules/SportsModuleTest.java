package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.world.modules.SportsModule.Pitch.PitchTexFunction;


public class SportsModuleTest {

	@Test
	public void testPitchTexFunction() {

		PitchTexFunction texFunction = new PitchTexFunction(
				NULL_VECTOR, new VectorXZ(2, 0), new VectorXZ(0, 1));

		assertAlmostEquals(new VectorXZ(0, 0),
				texFunction.apply(asList(new VectorXYZ(0, 0, 0)), null).get(0));
		assertAlmostEquals(new VectorXZ(0, 1),
				texFunction.apply(asList(new VectorXYZ(2, 0, 0)), null).get(0));
		assertAlmostEquals(new VectorXZ(1, 0),
				texFunction.apply(asList(new VectorXYZ(0, 0, 1)), null).get(0));
		assertAlmostEquals(new VectorXZ(1, 1),
				texFunction.apply(asList(new VectorXYZ(2, 0, 1)), null).get(0));
		assertAlmostEquals(new VectorXZ(0, .5),
				texFunction.apply(asList(new VectorXYZ(1, 0, 0)), null).get(0));
		assertAlmostEquals(new VectorXZ(2, 2),
				texFunction.apply(asList(new VectorXYZ(4, 0, 2)), null).get(0));
		assertAlmostEquals(new VectorXZ(0, -1),
				texFunction.apply(asList(new VectorXYZ(-2, 0, 0)), null).get(0));

		texFunction = new PitchTexFunction(
				NULL_VECTOR, new VectorXZ(0, -3), new VectorXZ(2, 0));

		assertAlmostEquals(new VectorXZ(0, 0),
				texFunction.apply(asList(new VectorXYZ(0, 0, 0)), null).get(0));
		assertAlmostEquals(new VectorXZ(0, 1),
				texFunction.apply(asList(new VectorXYZ(0, 0, -3)), null).get(0));
		assertAlmostEquals(new VectorXZ(1, 0),
				texFunction.apply(asList(new VectorXYZ(2, 0, 0)), null).get(0));
		assertAlmostEquals(new VectorXZ(1, 1),
				texFunction.apply(asList(new VectorXYZ(2, 0, -3)), null).get(0));

	}

}
