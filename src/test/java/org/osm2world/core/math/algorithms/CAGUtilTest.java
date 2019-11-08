package org.osm2world.core.math.algorithms;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertSame;
import static org.osm2world.core.test.TestUtil.assertSameCyclicOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;

public class CAGUtilTest {

	@Test
	public void testSubtractPolygons() {

		List<VectorXZ> outline = asList(
				new VectorXZ( 1,-2),
				new VectorXZ( 1, 2),
				new VectorXZ(-1, 2),
				new VectorXZ(-1,-2),
				new VectorXZ( 1,-2)
		);

		List<VectorXZ> subOutline = asList(
				new VectorXZ( 2,-1),
				new VectorXZ( 0,-1),
				new VectorXZ( 0, 1),
				new VectorXZ( 2, 1),
				new VectorXZ( 2,-1)
		);

		List<PolygonWithHolesXZ> results = new ArrayList<PolygonWithHolesXZ>(
				CAGUtil.subtractPolygons(
						new SimplePolygonXZ(outline),
						Arrays.asList(new SimplePolygonXZ(subOutline))));

		assertSame(1, results.size());

		assertSameCyclicOrder(true, results.get(0).getOuter().getVertices(),
				new VectorXZ( 1,-2),
				new VectorXZ( 1,-1),
				new VectorXZ( 0,-1),
				new VectorXZ( 0, 1),
				new VectorXZ( 1, 1),
				new VectorXZ( 1, 2),
				new VectorXZ(-1, 2),
				new VectorXZ(-1,-2));

	}

	@Test
	public void testSubtractPolygonsCommonNode() {

		List<VectorXZ> outline = asList(
				new VectorXZ( 1,-2),
				new VectorXZ( 1, 2),
				new VectorXZ(-1, 2),
				new VectorXZ(-1,-2),
				new VectorXZ( 1,-2)
		);

		List<VectorXZ> subOutline1 = asList(
				new VectorXZ( 2,-1),
				new VectorXZ( 0,-1),
				new VectorXZ( 0, 0),
				new VectorXZ( 2, 0),
				new VectorXZ( 2,-1)
		);

		List<VectorXZ> subOutline2 = asList(
				new VectorXZ( 2, 0),
				new VectorXZ( 0, 0),
				new VectorXZ( 0, 1),
				new VectorXZ( 2, 1),
				new VectorXZ( 2, 0)
		);

		List<PolygonWithHolesXZ> results = new ArrayList<PolygonWithHolesXZ>(
				CAGUtil.subtractPolygons(
						new SimplePolygonXZ(outline),
						Arrays.asList(
								new SimplePolygonXZ(subOutline1),
								new SimplePolygonXZ(subOutline2))));

		assertSame(1, results.size());

		List<VectorXZ> res = results.get(0).getOuter().getVertices();

		assertSameCyclicOrder(true, res,
				new VectorXZ( 1,-2),
				new VectorXZ( 1,-1),
				new VectorXZ( 0,-1),
				new VectorXZ( 0, 0),
				new VectorXZ( 0, 1),
				new VectorXZ( 1, 1),
				new VectorXZ( 1, 2),
				new VectorXZ(-1, 2),
				new VectorXZ(-1,-2));

	}

	@Test
	public void testsubtractPolygonsConvex() {

		List<VectorXZ> outline = asList(
				new VectorXZ( 1,-2),
				new VectorXZ( 1, 2),
				new VectorXZ(-1,-1),
				new VectorXZ(-1,-2),
				new VectorXZ( 1,-2)
		);

		List<VectorXZ> subOutline = asList(
				new VectorXZ( 0,-3),
				new VectorXZ(-2,-3),
				new VectorXZ(-2, 0),
				new VectorXZ( 0, 0),
				new VectorXZ( 0,-3)
		);

		List<PolygonWithHolesXZ> results = new ArrayList<PolygonWithHolesXZ>(
				CAGUtil.subtractPolygons(
						new SimplePolygonXZ(outline),
						Arrays.asList(new SimplePolygonXZ(subOutline))));

		assertSame(1, results.size());

		List<VectorXZ> res = results.get(0).getOuter().getVertices();

		assertSameCyclicOrder(true, res,
				new VectorXZ( 1,-2),
				new VectorXZ( 1, 2),
				new VectorXZ(-1/3.0, 0),
				new VectorXZ( 0, 0),
				new VectorXZ( 0,-2));

	}

	@Test
	public void testSubtractPolygonsHole() {

		List<VectorXZ> outline = asList(
				new VectorXZ(-2, -2),
				new VectorXZ(+2, -2),
				new VectorXZ(+2, +2),
				new VectorXZ(-2, +2),
				new VectorXZ(-2, -2)
		);

		List<VectorXZ> subOutline = asList(
				new VectorXZ(-3, -3),
				new VectorXZ(+3, -3),
				new VectorXZ(+3, +3),
				new VectorXZ(-3, +3),
				new VectorXZ(-3, -3)
		);

		List<VectorXZ> subOutlineHole = asList(
				new VectorXZ(-1, -1),
				new VectorXZ(+1, -1),
				new VectorXZ(+1, +1),
				new VectorXZ(-1, +1),
				new VectorXZ(-1, -1)
		);

		List<PolygonWithHolesXZ> results = new ArrayList<PolygonWithHolesXZ>(
				CAGUtil.subtractPolygons(
						new SimplePolygonXZ(outline),
						asList(new PolygonWithHolesXZ(new SimplePolygonXZ(subOutline),
								asList(new SimplePolygonXZ(subOutlineHole))))));

		assertSame(1, results.size());

		assertSameCyclicOrder(true, results.get(0).getOuter().getVertices(),
				new VectorXZ(-1, -1),
				new VectorXZ(+1, -1),
				new VectorXZ(+1, +1),
				new VectorXZ(-1, +1));

	}

}
