package org.osm2world.scene.texcoord;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.scene.material.TextureDataDimensions;

/**
 * creates texture coordinates for individual triangles that
 * orient the texture based on each triangle's downward slope.
 *
 * TODO: introduce face requirement?
 */
public record SlopedTrianglesTexCoordFunction(TextureDataDimensions textureDimensions) implements TexCoordFunction {

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {

		if (vs.size() % 3 != 0) {
			throw new IllegalArgumentException("not a set of triangles");
		}

		List<VectorXZ> result = new ArrayList<>(vs.size());

		List<Double> knownAngles = new ArrayList<>();

		for (int i = 0; i < vs.size() / 3; i++) {

			//TODO avoid creating a temporary triangle
			TriangleXYZ triangle = new TriangleXYZ(vs.get(3*i), vs.get(3*i+1), vs.get(3*i+2));

			VectorXZ normalXZProjection = triangle.getNormal().xz();

			double downAngle = 0;

			if (normalXZProjection.x != 0 || normalXZProjection.z != 0) {

				downAngle = normalXZProjection.angle();

				//try to avoid differences between triangles of the same face

				Double similarKnownAngle = null;

				for (double knownAngle : knownAngles) {
					if (abs(downAngle - knownAngle) < 0.02) {
						similarKnownAngle = knownAngle;
						break;
					}
				}

				if (similarKnownAngle == null) {
					knownAngles.add(downAngle);
				} else {
					downAngle = similarKnownAngle;
				}

			}

			for (VectorXYZ v : triangle.verticesNoDup()) {
				VectorXZ baseTexCoord = v.rotateY(-downAngle).xz();
				VectorXZ texCoord = new VectorXZ(
						-baseTexCoord.x / textureDimensions.width(),
						-baseTexCoord.z / textureDimensions.height());
				result.add(textureDimensions.applyPadding(texCoord));
			}

		}

		return result;

	}

}