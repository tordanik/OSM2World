package org.osm2world.world.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.osm2world.test.TestUtil.assertAlmostEquals;
import static org.osm2world.util.ListUtil.getFirst;
import static org.osm2world.util.ListUtil.getLast;

import java.util.List;

import org.junit.Test;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolylineXZ;

public class BridgeModuleTest {

	@Test
	public void testCreateCenterlineBetween_straightEdges() {

		var edge0 = new LineSegmentXZ(new VectorXZ(-5, 0), new VectorXZ(-5, 10));
		var edge1A = new LineSegmentXZ(new VectorXZ(3, 0), new VectorXZ(3, 10));

		PolylineXZ resultA = BridgeModule.Bridge.calculateCenterlineBetween(edge0, edge1A);

		assertNotNull(resultA);
		List<VectorXZ> expected = List.of(new VectorXZ(-1, 0), new VectorXZ(-1, 10));
		assertAlmostEquals(expected, resultA.vertices());

		var edge1B = new PolylineXZ(new VectorXZ(3, 0), new VectorXZ(3, 2), new VectorXZ(3, 10));
		PolylineXZ resultB = BridgeModule.Bridge.calculateCenterlineBetween(edge0, edge1B);

		assertNotNull(resultB);
		assertAlmostEquals(expected.get(0), getFirst(resultB.vertices()));
		assertAlmostEquals(expected.get(1), getLast(resultB.vertices()));
		assertEquals(10.0, resultB.getLength(), 0.01);

	}

	@Test
	public void testCreateCenterlineBetween_corner() {

		var edge0 = new PolylineXZ(new VectorXZ(0, 0), new VectorXZ(0, 5), new VectorXZ(-5, 5));
		var edge1 = new PolylineXZ(new VectorXZ(5, 0), new VectorXZ(5, 10), new VectorXZ(-5, 10));

		PolylineXZ result = BridgeModule.Bridge.calculateCenterlineBetween(edge0, edge1);

		assertNotNull(result);
		assertEquals(15, result.getLength(), 0.1);

	}

}
