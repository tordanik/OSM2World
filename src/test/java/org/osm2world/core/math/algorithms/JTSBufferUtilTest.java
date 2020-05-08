package org.osm2world.core.math.algorithms;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.osm2world.core.math.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.core.math.algorithms.JTSBufferUtil.bufferPolygon;
import static org.osm2world.core.test.TestUtil.assertSameCyclicOrder;

import java.util.List;

import org.junit.Test;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;

public class JTSBufferUtilTest {

	@Test
	public void testBufferPolygon_simpleSquare() {

		VectorXZ center = new VectorXZ(20, -3);
		SimplePolygonXZ input = asSimplePolygon(new AxisAlignedRectangleXZ(center, 10, 10));

		{
			List<PolygonWithHolesXZ> result0 = bufferPolygon(input, 0);
			assertEquals(1, result0.size());
			assertSameCyclicOrder(true, input.getVertexList(), result0.get(0).getVertexList());
		} {
			List<PolygonWithHolesXZ> resultGrow5 = bufferPolygon(input, 5);
			assertEquals(1, resultGrow5.size());
			assertSameCyclicOrder(true, new AxisAlignedRectangleXZ(center, 20, 20).getVertexList(),
					resultGrow5.get(0).getVertexList());
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
			assertSameCyclicOrder(true, outer.getVertexList(), result0.get(0).getOuter().getVertexList());
			assertSameCyclicOrder(true, inner.getVertexList(), result0.get(0).getHoles().get(0).getVertexList());
		} {
			List<PolygonWithHolesXZ> resultGrow1 = bufferPolygon(input, 1);
			assertEquals(1, resultGrow1.size());
			assertSameCyclicOrder(true, new AxisAlignedRectangleXZ(center, 12, 12).getVertexList(),
					resultGrow1.get(0).getOuter().getVertexList());
			assertSameCyclicOrder(true, new AxisAlignedRectangleXZ(center, 1, 1).getVertexList(),
					resultGrow1.get(0).getHoles().get(0).getVertexList());
		} {
			List<PolygonWithHolesXZ> resultShrink1 = bufferPolygon(input, -1);
			assertEquals(1, resultShrink1.size());
			assertSameCyclicOrder(true, new AxisAlignedRectangleXZ(center, 8, 8).getVertexList(),
					resultShrink1.get(0).getOuter().getVertexList());
			assertSameCyclicOrder(true, new AxisAlignedRectangleXZ(center, 5, 5).getVertexList(),
					resultShrink1.get(0).getHoles().get(0).getVertexList());
		} {
			List<PolygonWithHolesXZ> resultShrink20 = bufferPolygon(input, -20);
			assertTrue(resultShrink20.isEmpty());
		}

	}

}
