package org.osm2world.math.algorithms;

import java.util.Collection;
import java.util.List;

import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.TriangleXZ;

public class JTSTriangulationUtilTest extends AbstractTriangulationUtilTest {

	@Override
	protected Collection<TriangleXZ> triangulate(PolygonShapeXZ p, Collection<VectorXZ> points) {
		return JTSTriangulationUtil.triangulate(p.getOuter(), p.getHoles(), List.of(), points);
	}

}