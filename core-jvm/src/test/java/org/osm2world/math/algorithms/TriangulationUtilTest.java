package org.osm2world.math.algorithms;

import java.util.Collection;

import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.TriangleXZ;

public class TriangulationUtilTest extends AbstractTriangulationUtilTest {

	@Override
	protected Collection<TriangleXZ> triangulate(PolygonShapeXZ p, Collection<VectorXZ> points) {
		return TriangulationUtil.triangulate(p.getOuter(), p.getHoles(), points);
	}

}