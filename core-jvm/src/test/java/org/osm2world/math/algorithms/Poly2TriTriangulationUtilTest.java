package org.osm2world.math.algorithms;

import java.util.Collection;
import java.util.List;

import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.TriangleXZ;
import org.osm2world.util.exception.TriangulationException;

public class Poly2TriTriangulationUtilTest extends AbstractTriangulationUtilTest {

	@Override
	protected Collection<TriangleXZ> triangulate(PolygonShapeXZ p, Collection<VectorXZ> points) {
		try {
			return Poly2TriTriangulationUtil.triangulate(p.getOuter(), p.getHoles(), List.of(), List.of());
		} catch (TriangulationException e) {
			throw new RuntimeException(e);
		}
	}

}