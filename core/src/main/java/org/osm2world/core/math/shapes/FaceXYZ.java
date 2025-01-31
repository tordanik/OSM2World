package org.osm2world.core.math.shapes;

import static java.lang.Math.abs;
import static org.osm2world.core.math.shapes.FlatSimplePolygonShapeXYZ.triangleOnFace;

import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.util.exception.InvalidGeometryException;

/**
 * a simple polygon where all vertices are in the same plane
 */
public class FaceXYZ extends PolygonXYZ implements FlatSimplePolygonShapeXYZ {

	private final VectorXYZ normal;

	public FaceXYZ(List<VectorXYZ> vertexLoop) {

		super(vertexLoop);

		/* calculate the normal vector of the triangle defined by 3 non-collinear outline vertices */

		normal = triangleOnFace(vertexLoop).getNormal();

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
