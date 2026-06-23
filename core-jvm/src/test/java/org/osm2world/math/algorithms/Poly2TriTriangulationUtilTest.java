package org.osm2world.math.algorithms;

import java.util.List;

import org.junit.Test;
import org.osm2world.util.exception.TriangulationException;

public class Poly2TriTriangulationUtilTest extends AbstractTriangulationUtilTest {

	@Test
	public void testTriangulate() {
		testTriangulate(p -> {
			try {
				return Poly2TriTriangulationUtil.triangulate(p.getOuter(), p.getHoles(), List.of(), List.of());
			} catch (TriangulationException e) {
				throw new RuntimeException(e);
			}
		});
	}

}