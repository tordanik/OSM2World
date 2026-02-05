package org.osm2world.world.modules.building;

import static java.util.Arrays.asList;
import static org.osm2world.math.VectorXYZ.Z_UNIT;
import static org.osm2world.math.VectorXYZ.addYList;
import static org.osm2world.scene.material.DefaultMaterials.BRICK;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;

public class WallSurfaceTest {

	/** a basic 20 x 10 meter flat wall for testing */
	static WallSurface rectangularWallSurface;

	{
		List<VectorXYZ> lowerBoundary = asList(
				new VectorXYZ(-10, 0, 5),
				new VectorXYZ(-3, 0, 5),
				new VectorXYZ(10, 0, 5));
		List<VectorXYZ> upperBoundary = addYList(lowerBoundary, 10);

		rectangularWallSurface = new WallSurface(BRICK.get(config), lowerBoundary, upperBoundary);
	}

	@Test
	public void testWallCoord() {

		List<Pair<VectorXYZ, VectorXZ>> coordMapping = asList(
				Pair.of(new VectorXYZ(-10, 0, 5), new VectorXZ(0, 0)),
				Pair.of(new VectorXYZ(10, 0, 5), new VectorXZ(20, 0)),
				Pair.of(new VectorXYZ(0, 2, 5), new VectorXZ(10, 2))
		);

		for (Pair<VectorXYZ, VectorXZ> c : coordMapping) {
			assertAlmostEquals(c.getValue(), rectangularWallSurface.toWallCoord(c.getKey()));
			assertAlmostEquals(c.getKey(), rectangularWallSurface.convertTo3D(c.getValue()));
		}

	}

	@Test
	public void testNormal() {

		for (VectorXZ testPoint : asList(new VectorXZ(0, 0), new VectorXZ(5, 5), new VectorXZ(20, 2))) {
			assertAlmostEquals(Z_UNIT.invert(), rectangularWallSurface.normalAt(testPoint));
		}

	}

}
