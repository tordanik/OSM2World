package org.osm2world.core.target.common.texcoord;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.FaceXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

/**
 * fits an image onto a flat polygon.
 * Vertices must represent the vertex loop of a {@link FaceXYZ}
 */
public class FaceFitTexCoordFunction implements TexCoordFunction {

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {

		List<VectorXZ> result = new ArrayList<>(vs.size());

		FaceXYZ face = new FaceXYZ(vs);
		SimplePolygonXZ faceXZ = face.toFacePlane(face);
		AxisAlignedRectangleXZ faceBbox = faceXZ.boundingBox();

		for (VectorXZ v : faceXZ.vertices()) {
			VectorXZ vRelative = v.subtract(faceBbox.bottomLeft());
			result.add(new VectorXZ(vRelative.x / faceBbox.sizeX(), vRelative.z / faceBbox.sizeZ()));
		}

		return result;

	}

}
