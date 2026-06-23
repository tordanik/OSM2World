package org.osm2world.math.algorithms;

import java.util.List;

import org.junit.Test;

public class JTSTriangulationUtilTest extends AbstractTriangulationUtilTest {

	@Test
	public void testTriangulate() {
		testTriangulate(p -> JTSTriangulationUtil.triangulate(p.getOuter(), p.getHoles(), List.of(), List.of()));
	}

}