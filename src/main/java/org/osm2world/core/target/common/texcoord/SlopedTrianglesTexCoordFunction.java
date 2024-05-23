package org.osm2world.core.target.common.texcoord;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.TextureDataDimensions;

/**
 * creates texture coordinates for individual triangles that
 * orient the texture based on each triangle's downward slope.
 *
 * TODO: introduce face requirement?
 */
public class SlopedTrianglesTexCoordFunction implements TexCoordFunction {

	public final TextureDataDimensions textureDimensions;

	public SlopedTrianglesTexCoordFunction(TextureDataDimensions textureDimensions) {
		this.textureDimensions = textureDimensions;
	}

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {

		if (vs.size() % 3 != 0) {
			throw new IllegalArgumentException("not a set of triangles");
		}

		List<VectorXZ> result = new ArrayList<>(vs.size());

		List<Double> knownAngles = new ArrayList<Double>();

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
				result.add(new VectorXZ(
						-baseTexCoord.x / textureDimensions.width(),
						-baseTexCoord.z / textureDimensions.height()));
			}

		}

		return result;

	}

}