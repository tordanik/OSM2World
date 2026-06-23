package org.osm2world.math.algorithms;

import org.junit.Test;

public class TriangulationUtilTest extends AbstractTriangulationUtilTest {

	@Test
	public void testTriangulate() {
		testTriangulate(TriangulationUtil::triangulate);
	}

}