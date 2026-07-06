package org.osm2world.map_elevation.creation;

import static org.junit.Assert.assertEquals;
import static org.osm2world.map_elevation.creation.LocalCellInterpolator.interpolateEleFromSurroundingSites;

import java.util.List;

import org.junit.Test;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;

public class LocalCellInterpolatorTest {

	@Test
	public void testInterpolateEleFromSurroundingSites() {

		List<VectorXYZ> surroundingSites = List.of(
			new VectorXYZ(0, 10, 0),
			new VectorXYZ(1, 20, 0),
			new VectorXYZ(0, 0, 1),
			new VectorXYZ(1, 0, 1)
		);

		for (VectorXYZ pos : surroundingSites) {
			var result = interpolateEleFromSurroundingSites(pos.xz(), surroundingSites);
			assertEquals(pos.y, result.y, 0.01);
		}

		assertEquals(12, interpolateEleFromSurroundingSites(new VectorXZ(0.2, 0), surroundingSites).y, 0.01);
		assertEquals(15, interpolateEleFromSurroundingSites(new VectorXZ(0.5, 0), surroundingSites).y, 0.01);
		assertEquals(18, interpolateEleFromSurroundingSites(new VectorXZ(0.8, 0), surroundingSites).y, 0.01);

		assertEquals(6, interpolateEleFromSurroundingSites(new VectorXZ(0.2, 0.5), surroundingSites).y, 0.01);
		assertEquals(7.5, interpolateEleFromSurroundingSites(new VectorXZ(0.5, 0.5), surroundingSites).y, 0.01);
		assertEquals(9, interpolateEleFromSurroundingSites(new VectorXZ(0.8, 0.5), surroundingSites).y, 0.01);

	}

}
