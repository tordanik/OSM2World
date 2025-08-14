package org.osm2world.scene.texcoord;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.math.shapes.FaceXYZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.scene.material.TextureDataDimensions;

/**
 * fits an image onto a flat polygon.
 * Vertices must represent the vertex loop of a {@link FaceXYZ}
 */
public record FaceFitTexCoordFunction(TextureDataDimensions textureDimensions) implements TexCoordFunction {

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {

		List<VectorXZ> result = new ArrayList<>(vs.size());

		FaceXYZ face = new FaceXYZ(vs);
		SimplePolygonXZ faceXZ = face.toFacePlane(face);
		AxisAlignedRectangleXZ faceBbox = faceXZ.boundingBox();

		for (VectorXZ v : faceXZ.vertices()) {
			VectorXZ vRelative = v.subtract(faceBbox.bottomLeft());
			VectorXZ rawTexCoord = new VectorXZ(vRelative.x / faceBbox.sizeX(), vRelative.z / faceBbox.sizeZ());
			result.add(textureDimensions.applyPadding(rawTexCoord));
		}

		return result;

	}

}
