package org.osm2world.core.math.shapes;

import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.core.math.VectorXZ;

public class CircleXZTest extends PolylineXZTest {

	@Test
	public void testTransform() {

		CircleXZ input = new CircleXZ(new VectorXZ(0, 0), 5);
		CircleXZ expected = new CircleXZ(new VectorXZ(10, 3), 5);

		assertAlmostEquals(expected.vertices(), input.transform(v -> v.add(10, 3)).vertices());

	}

}
