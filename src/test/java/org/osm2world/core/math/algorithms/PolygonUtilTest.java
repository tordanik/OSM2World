package org.osm2world.core.math.algorithms;

import static java.util.Arrays.asList;
import static org.osm2world.core.test.TestUtil.assertSameCyclicOrder;

import org.junit.Test;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;

public class PolygonUtilTest {

	@Test
	public void testConvexHull() {

		SimplePolygonXZ p = new SimplePolygonXZ(asList(
				new VectorXZ( -5, 0),
				new VectorXZ( -4, 1),
				new VectorXZ( -3, 3),
				new VectorXZ( -2, 2),
				new VectorXZ( -1, 1),
				new VectorXZ(  0, 4),
				new VectorXZ( +1, 4),
				new VectorXZ( +2, 1),
				new VectorXZ( +3, 2),
				new VectorXZ( +4, 2),
				new VectorXZ( +5, 0),
				new VectorXZ( -5, 0)));

		SimplePolygonXZ hull = PolygonUtil.convexHull(p);

		assertSameCyclicOrder(false, hull.getVertices(),
				new VectorXZ( -5, 0),
				new VectorXZ( -3, 3),
				new VectorXZ(  0, 4),
				new VectorXZ( +1, 4),
				new VectorXZ( +4, 2),
				new VectorXZ( +5, 0));

	}

	@Test
	public void testConvexHull2() {

		SimplePolygonXZ p = new SimplePolygonXZ(asList(
				new VectorXZ( -2.0,  0.0),
				new VectorXZ( -0.5, -0.5),
				new VectorXZ(  0.0, -2.0),
				new VectorXZ( +0.5, -0.5),
				new VectorXZ( +2.0,  0.0),
				new VectorXZ(  0.0, -0.5),
				new VectorXZ( -2.0,  0.0)));

		SimplePolygonXZ hull = PolygonUtil.convexHull(p);

		assertSameCyclicOrder(false, hull.getVertices(),
				new VectorXZ( -2.0,  0.0),
				new VectorXZ(  0.0, -2.0),
				new VectorXZ( +2.0,  0.0));

	}

	@Test
	public void testConvexHull3() {

		SimplePolygonXZ p = new SimplePolygonXZ(asList(
				new VectorXZ(-1, 0),
				new VectorXZ(+1, 0),
				new VectorXZ( 0, 1),
				new VectorXZ(-1, 0)));

		SimplePolygonXZ hull = PolygonUtil.convexHull(p);

		assertSameCyclicOrder(false, hull.getVertices(),
					new VectorXZ(-1, 0),
				new VectorXZ(+1, 0),
				new VectorXZ( 0, 1));

	}

	@Test
	public void testConvexHull4() {

		SimplePolygonXZ p = new SimplePolygonXZ(asList(
				new VectorXZ(0, 0),
				new VectorXZ(0, 2),
				new VectorXZ(1, 1),
				new VectorXZ(2, 2),
				new VectorXZ(2, 0),
				new VectorXZ(0, 0)));

		SimplePolygonXZ hull = PolygonUtil.convexHull(p);

		assertSameCyclicOrder(false, hull.getVertices(),
				new VectorXZ(0, 0),
				new VectorXZ(0, 2),
				new VectorXZ(2, 2),
				new VectorXZ(2, 0));

	}

	/** simple test cases where the bounding box is identical to the polygon */
	@Test
	public void testMinimumBoundingBox() {

		//axis aligned case

		SimplePolygonXZ p1 = new SimplePolygonXZ(asList(
				new VectorXZ(0, 0),
				new VectorXZ(1, 0),
				new VectorXZ(1, 1),
				new VectorXZ(0, 1),
				new VectorXZ(0, 0)));

		SimplePolygonXZ bbox1 = PolygonUtil.minimumBoundingBox(p1);

		assertSameCyclicOrder(true, bbox1.getVertices(),
				new VectorXZ(0, 0),
				new VectorXZ(1, 0),
				new VectorXZ(1, 1),
				new VectorXZ(0, 1));

		//not aligned to axis

		SimplePolygonXZ p2 = new SimplePolygonXZ(asList(
				new VectorXZ(+.5,   0),
				new VectorXZ(  0, +.5),
				new VectorXZ(-.5,   0),
				new VectorXZ(  0, -.5),
				new VectorXZ(+.5,   0)));

		SimplePolygonXZ bbox2 = PolygonUtil.minimumBoundingBox(p2);

		assertSameCyclicOrder(true, bbox2.getVertices(),
				new VectorXZ(+.5,   0),
				new VectorXZ(  0, +.5),
				new VectorXZ(-.5,   0),
				new VectorXZ(  0, -.5));

	}

	@Test
	public void testMinimumBoundingBox2() {

		SimplePolygonXZ p = new SimplePolygonXZ(asList(
				new VectorXZ(1, 1),
				new VectorXZ(2, 1),
				new VectorXZ(4, 3),
				new VectorXZ(3, 3),
				new VectorXZ(1, 1)));

		SimplePolygonXZ bbox = PolygonUtil.minimumBoundingBox(p);

		assertSameCyclicOrder(true, bbox.getVertices(),
				new VectorXZ(1, 1),
				new VectorXZ(3.5, 3.5),
				new VectorXZ(4, 3),
				new VectorXZ(1.5, 0.5));

	}

}
