package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.osm2world.core.math.VectorXYZ.*;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.transformShape;

import java.util.List;

import org.junit.Test;
import org.osm2world.core.math.VectorXYZ;

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

}
