package org.osm2world.core.math;

import static java.lang.Math.abs;

import java.util.List;

/**
 * a simple polygon where all vertices are in the same plane
 */
public class FaceXYZ extends PolygonXYZ implements FlatSimplePolygonShapeXYZ {

	private final VectorXYZ normal;

	public FaceXYZ(List<VectorXYZ> vertexLoop) {

		super(vertexLoop);

		/* find three polygon vertices that are not collinear (for normal vector calculation) */

		final VectorXYZ v1 = vertexLoop.get(0);
		final VectorXYZ v2 = vertexLoop.get(1);

		VectorXYZ v3 = null;
		for (int i = 2; i < vertexLoop.size() - 1; i++) {
			VectorXYZ vTest = vertexLoop.get(i);
			if (vTest.subtract(v1).angleTo(vTest.subtract(v2)) > 0.001) {
				v3 = vTest;
			}
		}

		if (v3 == null) {
			throw new InvalidGeometryException("Points of face are collinear: " + vertexLoop);
		}

		/* calculate the normal vector of the triangle defined by these 3 points */

		normal = new TriangleXYZ(v1, v2, v3).getNormal();

		/* check that the vertices lie in a plane, throw exception otherwise */

		for (VectorXYZ v : vertexLoop) {
			if (abs(rotateToOrFromFacePlane(v, true).y - getCenter().y) > 0.01) {
				throw new InvalidGeometryException("face vertices do not lie in a plane");
			}
		}

	}

	@Override
	public VectorXYZ getNormal() {
		return normal;
	}

}
