package org.osm2world.world.modules;

import static java.util.Arrays.asList;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.target.common.material.TextureDataDimensions;
import org.osm2world.world.modules.SportsModule.Pitch.PitchTexFunction;


public class SportsModuleTest {

	@Test
	public void testPitchTexFunction() {

		PitchTexFunction texFunction = new PitchTexFunction(
				NULL_VECTOR, new VectorXZ(2, 0), new VectorXZ(0, 1), new TextureDataDimensions(1, 1));

		assertAlmostEquals(0, 0, texFunction.apply(asList(new VectorXYZ(0, 0, 0))).get(0));
		assertAlmostEquals(0, 1, texFunction.apply(asList(new VectorXYZ(2, 0, 0))).get(0));
		assertAlmostEquals(1, 0, texFunction.apply(asList(new VectorXYZ(0, 0, 1))).get(0));
		assertAlmostEquals(1, 1, texFunction.apply(asList(new VectorXYZ(2, 0, 1))).get(0));
		assertAlmostEquals(0, .5, texFunction.apply(asList(new VectorXYZ(1, 0, 0))).get(0));
		assertAlmostEquals(2, 2, texFunction.apply(asList(new VectorXYZ(4, 0, 2))).get(0));
		assertAlmostEquals(0, -1, texFunction.apply(asList(new VectorXYZ(-2, 0, 0))).get(0));

		texFunction = new PitchTexFunction(NULL_VECTOR, new VectorXZ(0, -3), new VectorXZ(2, 0), new TextureDataDimensions(1, 1));

		assertAlmostEquals(0, 0, texFunction.apply(asList(new VectorXYZ(0, 0, 0))).get(0));
		assertAlmostEquals(0, 1, texFunction.apply(asList(new VectorXYZ(0, 0, -3))).get(0));
		assertAlmostEquals(1, 0, texFunction.apply(asList(new VectorXYZ(2, 0, 0))).get(0));
		assertAlmostEquals(1, 1, texFunction.apply(asList(new VectorXYZ(2, 0, -3))).get(0));

	}

}
