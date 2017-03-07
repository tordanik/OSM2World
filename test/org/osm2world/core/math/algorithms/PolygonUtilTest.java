package org.osm2world.core.math.algorithms;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.osm2world.core.math.PolygonXZ;
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
	
		PolygonXZ hull = PolygonUtil.convexHull(p);
		
		assertEquals(new VectorXZ( -5, 0), hull.getVertex(0));
		assertEquals(new VectorXZ( -3, 3), hull.getVertex(1));
		assertEquals(new VectorXZ(  0, 4), hull.getVertex(2));
		assertEquals(new VectorXZ( +1, 4), hull.getVertex(3));
		assertEquals(new VectorXZ( +4, 2), hull.getVertex(4));
		assertEquals(new VectorXZ( +5, 0), hull.getVertex(5));
		
	}
	
}
