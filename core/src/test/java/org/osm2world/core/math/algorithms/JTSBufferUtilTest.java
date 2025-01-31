package org.osm2world.core.math.algorithms;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osm2world.core.math.algorithms.JTSBufferUtil.bufferPolygon;
import static org.osm2world.core.math.shapes.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.core.test.TestUtil.assertSameCyclicOrder;

import java.util.List;

import org.junit.Test;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.core.math.shapes.PolygonWithHolesXZ;
import org.osm2world.core.math.shapes.SimplePolygonXZ;

public class JTSBufferUtilTest {

	@Test
	public void testBufferPolygon_simpleSquare() {

		VectorXZ center = new VectorXZ(20, -3);
		SimplePolygonXZ input = asSimplePolygon(new AxisAlignedRectangleXZ(center, 10, 10));

		{
			List<PolygonWithHolesXZ> result0 = bufferPolygon(input, 0);
			assertEquals(1, result0.size());
			assertSameCyclicOrder(true, input.vertices(), result0.get(0).vertices());
		} {
			List<PolygonWithHolesXZ> resultGrow5 = bufferPolygon(input, 5);
			assertEquals(1, resultGrow5.size());
			assertSameCyclicOrder(true, new AxisAlignedRectangleXZ(center, 20, 20).vertices(),
					resultGrow5.get(0).vertices());
		} {
			List<PolygonWithHolesXZ> resultShrink20 = bufferPolygon(input, -20);
			assertTrue(resultShrink20.isEmpty());
		}

	}

	@Test
	public void testBufferPolygon_withHoles() {

		VectorXZ center = new VectorXZ(20, -3);

		SimplePolygonXZ outer = asSimplePolygon(new AxisAlignedRectangleXZ(center, 10, 10));
		SimplePolygonXZ inner = asSimplePolygon(new AxisAlignedRectangleXZ(center, 3, 3));
		PolygonWithHolesXZ input = new PolygonWithHolesXZ(outer, asList(inner));


		{
			List<PolygonWithHolesXZ> result0 = bufferPolygon(input, 0);
			assertEquals(1, result0.size());
			assertSameCyclicOrder(true, outer.vertices(), result0.get(0).getOuter().vertices());
			assertSameCyclicOrder(true, inner.vertices(), result0.get(0).getHoles().get(0).vertices());
		} {
			List<PolygonWithHolesXZ> resultGrow1 = bufferPolygon(input, 1);
			assertEquals(1, resultGrow1.size());
			assertSameCyclicOrder(true, new AxisAlignedRectangleXZ(center, 12, 12).vertices(),
					resultGrow1.get(0).getOuter().vertices());
			assertSameCyclicOrder(true, new AxisAlignedRectangleXZ(center, 1, 1).vertices(),
					resultGrow1.get(0).getHoles().get(0).vertices());
		} {
			List<PolygonWithHolesXZ> resultShrink1 = bufferPolygon(input, -1);
			assertEquals(1, resultShrink1.size());
			assertSameCyclicOrder(true, new AxisAlignedRectangleXZ(center, 8, 8).vertices(),
					resultShrink1.get(0).getOuter().vertices());
			assertSameCyclicOrder(true, new AxisAlignedRectangleXZ(center, 5, 5).vertices(),
					resultShrink1.get(0).getHoles().get(0).vertices());
		} {
			List<PolygonWithHolesXZ> resultShrink20 = bufferPolygon(input, -20);
			assertTrue(resultShrink20.isEmpty());
		}

	}

}
