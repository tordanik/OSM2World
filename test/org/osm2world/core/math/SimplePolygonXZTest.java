package org.osm2world.core.math;

import static java.util.Arrays.asList;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import org.junit.Test;

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
				
		assertAlmostEquals(VectorXZ.NULL_VECTOR, p1.getCentroid());
		assertAlmostEquals(VectorXZ.NULL_VECTOR, p1.reverse().getCentroid());

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
	
}
