package org.osm2world.scene.texcoord;

import static java.lang.Math.round;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.scene.material.TextureDataDimensions;

/**
 * creates texture coordinates for a triangle strip (alternating between
 * upper and lower vertex), based on the length along a wall from the
 * starting point, height of the vertex, and texture dimensions.
 *
 * This only works for vertices forming a triangle strip,
 * alternating between upper and lower vertex.
 */
public record StripWallTexCoordFunction(TextureDataDimensions textureDimensions, boolean fitWidth, boolean fitHeight)
		implements TexCoordFunction {

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {

		if (vs.size() % 2 == 1) {
			throw new IllegalArgumentException("not a triangle strip wall");
		}

		List<VectorXZ> result = new ArrayList<>(vs.size());

		/* calculate length of the wall (if needed later) */

		double totalLength = 0;

		if (fitWidth || textureDimensions.widthPerEntity() != null) {
			for (int i = 0; i+1 < vs.size(); i++) {
				totalLength += vs.get(i).distanceToXZ(vs.get(i+1));
			}
		}

		/* calculate number of repetitions in each dimension */

		double width, height;

		if (fitWidth) {
			width = totalLength;
		} else if (textureDimensions.widthPerEntity() != null) {
			long entities = Long.max(1, round(totalLength / textureDimensions.widthPerEntity()));
			double textureRepeats = entities / (textureDimensions.width() / textureDimensions.widthPerEntity());
			width = totalLength / textureRepeats;
		} else {
			width = textureDimensions.width();
		}

		if (textureDimensions.heightPerEntity() != null) {
			double totalHeight = IntStream.range(0, vs.size() / 2)
					.mapToDouble(i -> vs.get(2 * i).distanceTo(vs.get(2 * i + 1)))
					.max().getAsDouble();
			long entities = Long.max(1, round(totalHeight / textureDimensions.heightPerEntity()));
			double textureRepeats = entities / (textureDimensions.height() / textureDimensions.heightPerEntity());
			height = totalHeight / textureRepeats;
		} else {
			height = textureDimensions.height();
		}

		/* calculate texture coordinate list */

		double accumulatedLength = 0;

		for (int i = 0; i < vs.size(); i++) {

			VectorXYZ v = vs.get(i);

			// increase accumulated length after every second vector

			if (i > 0 && i % 2 == 0) {
				accumulatedLength += v.xz().distanceTo(vs.get(i-2).xz());
			}

			// calculate texture coords

			double s, t;

			s = accumulatedLength / width;

			if (fitHeight) {
				t = (i % 2 == 0) ? 1 : 0;
			} else {
				t = (i % 2 == 0) ? (v.distanceTo(vs.get(i+1))) / height : 0;
			}

			VectorXZ rawTexCoord = new VectorXZ(s, t);
			result.add(TexCoordUtil.applyPadding(rawTexCoord, textureDimensions));

		}

		return result;

	}

}