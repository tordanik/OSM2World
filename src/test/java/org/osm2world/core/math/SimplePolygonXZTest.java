package org.osm2world.core.math;

import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;

public class SimplePolygonXZTest {

	private SimplePolygonXZ p1 = new SimplePolygonXZ(asList(
			new VectorXZ(-1, -1),
			new VectorXZ(-1,  0),
			new VectorXZ(-1, +1),
			new VectorXZ(+1, +1),
			new VectorXZ(+1, -1),
			new VectorXZ(-1, -1)));

	private SimplePolygonXZ p2 = new SimplePolygonXZ(asList(
			new VectorXZ(-0.5, -0.5),
			new VectorXZ(-0.5, +1.5),
			new VectorXZ(+1.5, +1.5),
			new VectorXZ(+1.5, -0.5),
			new VectorXZ(-0.5, -0.5)));

	@Test
	public void testGetCentroid() {

		assertAlmostEquals(NULL_VECTOR, p1.getCentroid());
		assertAlmostEquals(NULL_VECTOR, p1.reverse().getCentroid());

		assertAlmostEquals(new VectorXZ(0.5, 0.5), p2.getCentroid());
		assertAlmostEquals(new VectorXZ(0.5, 0.5), p2.reverse().getCentroid());

	}

	@Test
	public void testGetArea() {

		assertAlmostEquals(4, p1.getArea());
		assertAlmostEquals(4, p1.reverse().getArea());

		assertAlmostEquals(4, p2.getArea());
		assertAlmostEquals(4, p2.reverse().getArea());

	}

	@Test
	public void testGetDiameter() {

		assertAlmostEquals(sqrt(8), p1.getDiameter());
		assertAlmostEquals(sqrt(8), p2.getDiameter());

	}

	@Test
	public void testGetSimplifiedPolygon() {

		assertEquals(p2, p2.getSimplifiedPolygon());

		assertEquals(4, p1.getSimplifiedPolygon().size());
		assertAlmostEquals(p1.getArea(), p1.getSimplifiedPolygon().getArea());

	}

	@Test
	public void testDistanceToSegments() {

		assertAlmostEquals(1, p1.distanceToSegments(NULL_VECTOR));

		assertAlmostEquals(0.5, p2.distanceToSegments(NULL_VECTOR));

	}

	@Test
	public void testShift() {

		SimplePolygonShapeXZ shiftP = p1.shift(VectorXZ.X_UNIT);

		assertSame(p1.size(), shiftP.getVertexList().size() - 1);
		assertAlmostEquals(new VectorXZ( 0, -1), shiftP.getVertexList().get(0));
		assertAlmostEquals(new VectorXZ( 0,  0), shiftP.getVertexList().get(1));
		assertAlmostEquals(new VectorXZ( 0, +1), shiftP.getVertexList().get(2));
		assertAlmostEquals(new VectorXZ( 2, +1), shiftP.getVertexList().get(3));
		assertAlmostEquals(new VectorXZ( 2, -1), shiftP.getVertexList().get(4));
		assertAlmostEquals(new VectorXZ( 0, -1), shiftP.getVertexList().get(5));

	}

}
