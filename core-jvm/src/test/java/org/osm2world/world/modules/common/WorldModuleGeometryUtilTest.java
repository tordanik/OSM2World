package org.osm2world.world.modules.common;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.osm2world.math.VectorXYZ.*;
import static org.osm2world.math.algorithms.GeometryUtil.trianglesFromTriangleStrip;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.transformShape;

import java.util.List;

import org.junit.Test;
import org.osm2world.math.VectorXYZ;

public class WorldModuleGeometryUtilTest {

	@Test
	public void testTransformShape() {

		List<VectorXYZ> vs = asList(new VectorXYZ(-1, 0, -1), new VectorXYZ(2, 0, 1));

		/* identity transform */

		assertEquals(vs, transformShape(vs, NULL_VECTOR, Z_UNIT, Y_UNIT));

		/* translation */

		assertEquals(asList(new VectorXYZ(9, 5, 1), new VectorXYZ(12, 5, 3)),
				transformShape(vs, new VectorXYZ(10, 5, 2), Z_UNIT, Y_UNIT));

		/* rotation */

		assertEquals(asList(new VectorXYZ(-1, 0, 1), new VectorXYZ(1, 0, -2)),
				transformShape(vs, NULL_VECTOR, X_UNIT, Y_UNIT));

		/* rotation and translation */

		assertEquals(asList(new VectorXYZ(-1, 5, 6), new VectorXYZ(1, 5, 3)),
				transformShape(vs, new VectorXYZ(0, 5, 5), X_UNIT, Y_UNIT));

	}

	@Test
	public void testCreateTriangleStripBetween() {

		List<VectorXYZ> left = List.of(new VectorXYZ(-1, 0, 0), new VectorXYZ(-1, 0, 5), new VectorXYZ(-1, 1, 15));
		List<VectorXYZ> right = List.of(new VectorXYZ(1, 0, 0), new VectorXYZ(1, 0, 6), new VectorXYZ(1, 1, 15));

		List<VectorXYZ> result = createTriangleStripBetween(left, right);

		assertEquals(List.of(left.get(0), right.get(0), left.get(1), right.get(1), left.get(2), right.get(2)), result);
		assertEquals(4, trianglesFromTriangleStrip(result).size());

	}

}
