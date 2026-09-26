package org.osm2world.scene.texcoord;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.math.Angle;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.scene.material.TextureDataDimensions;

/**
 * uses x and z vertex coords together with the texture's width and height
 * to place a texture. This function works for all geometries,
 * but steep inclines or even vertical walls produce odd-looking results.
 *
 * @param rotateAngle  optional angle by which the texture will be rotated in the plane
 */
public record GlobalXZTexCoordFunction(TextureDataDimensions textureDimensions, @Nullable Angle rotateAngle) implements TexCoordFunction {

	public GlobalXZTexCoordFunction(TextureDataDimensions textureDimensions) {
		this(textureDimensions, null);
	}

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {

		List<VectorXZ> result = new ArrayList<>(vs.size());

		for (VectorXYZ v : vs) {
			if (rotateAngle != null) {
				v = v.rotateY(-rotateAngle.radians);
			}
			result.add(textureDimensions.applyPadding(new VectorXZ(
							v.x / textureDimensions.width(),
							v.z / textureDimensions.height())));
		}

		return result;

	}

}
