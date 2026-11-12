package org.osm2world.scene.texcoord;

import static java.lang.Math.max;
import static java.lang.Math.round;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.scene.material.TextureDataDimensions;
import org.osm2world.scene.texcoord.StripCoordinates.StripPosition;

/**
 * creates texture coordinates for a horizontal strip, such as a road or railway, based on each vertex's position
 * within the strip (see {@link StripCoordinates}). Unlike {@link StripWallTexCoordFunction}, this works for any
 * triangulation of the strip, including triangles with vertices which are not on the strip's outlines.
 * At the outline vertices, the result is the same as that of {@link NamedTexCoordFunction#STRIP_FIT_HEIGHT}
 * for a triangle strip alternating between left and right outline.
 * TODO: Merge with StripWallTexCoordFunction
 */
public class StripPositionTexCoordFunction implements TexCoordFunction {

	private final StripCoordinates strip;

	public StripPositionTexCoordFunction(List<VectorXZ> left, List<VectorXZ> right) {
		this.strip = new StripCoordinates(left, right);
	}

	@Override
	public List<VectorXZ> apply(TextureDataDimensions textureDimensions, List<VectorXYZ> vs) {

		double totalLength = strip.getLength();

		double width; // horizontal width of the texture
		if (textureDimensions.widthPerEntity() != null) {
			long entities = max(1, round(totalLength / textureDimensions.widthPerEntity()));
			double textureRepeats = entities / (textureDimensions.width() / textureDimensions.widthPerEntity());
			width = totalLength / textureRepeats;
		} else {
			width = textureDimensions.width();
		}

		List<VectorXZ> result = new ArrayList<>(vs.size());
		for (VectorXYZ v : vs) {
			StripPosition position = strip.positionOf(v.xz());
			VectorXZ rawTexCoord = new VectorXZ(position.distanceAlong() / width, 1 - position.fractionAcross());
			result.add(textureDimensions.applyPadding(rawTexCoord));
		}
		return result;
	}

}
