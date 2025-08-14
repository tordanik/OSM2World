package org.osm2world.scene.material;

import static java.lang.Math.round;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.math.VectorXZ;
import org.osm2world.scene.texcoord.TexCoordFunction;

/**
 * the dimensions of a texture.
 * @param width  width of the texture in meters, greater than 0
 * @param height  height of the texture in meters, greater than 0
 * @param widthPerEntity  for textures containing distinct, repeating objects (e.g. floor tiles),
 *                        this is the width of one such object. Some calculations for texture coords will use this
 *                        to fit an integer number of such objects onto a surface.
 * @param heightPerEntity  vertical counterpart to widthPerEntity
 * @param padding  the fraction of the texture which represents padding, greater or equal 0 and less than 0.5.
 *                 Padding is only used for a minority of textures (0 otherwise), only for textures
 *                 which aren't repeated, and typically for textures which have a transparent border.
 *                 For example, if this value is 0.1, only the portion of the image between texture coordinates
 *                 0.1 and 0.9 (along both the height and width axis) represents actual content.
 *                 The goal of padding is to avoid colors bleeding into the border on coarse mip-map levels.
 */
public record TextureDataDimensions(double width, double height,
									@Nullable Double widthPerEntity,
									@Nullable Double heightPerEntity,
									double padding) {

	public TextureDataDimensions {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Illegal texture dimensions. Width: " + width + ", height: " + height);
		} else if (widthPerEntity != null && widthPerEntity <= 0 || heightPerEntity != null && heightPerEntity <= 0) {
			throw new IllegalArgumentException("Illegal per-entity texture dimensions.");
		} else if (padding < 0 || padding >= 0.5) {
			throw new IllegalArgumentException("Illegal texture dimensions. Padding: " + padding);
		}
	}

	public TextureDataDimensions(double width, double height) {
		this(width, height, null, null, 0);
	}

	/**
	 * takes raw texture coordinates (for an idealized 1m x 1m texture) and scales them to fit an actual texture
	 * based on these {@link TextureDataDimensions}.
	 * Also implements padding based on ({@link #padding()})
	 * and snapping (based on {@link #widthPerEntity()} and based on {@link #heightPerEntity()}).
	 *
	 * @param totalX  size of the range of X values, used for texture snapping, optional
	 * @param totalZ  size of the range of Z values, used for texture snapping, optional
	 */
	public List<VectorXZ> scaleTexCoords(List<VectorXZ> rawTexCoords,
			@Nullable Double totalX, @Nullable Double totalZ) {

		List<VectorXZ> result = new ArrayList<>(rawTexCoords.size());

		for (VectorXZ v : rawTexCoords) {

			double width = width();
			double height = height();

			if (totalX != null && widthPerEntity() != null) {
				long entities = Long.max(1, round(totalX / widthPerEntity()));
				double textureRepeats = entities / (width() / widthPerEntity());
				width = totalX / textureRepeats;
			}

			if (totalZ != null && heightPerEntity() != null) {
				long entities = Long.max(1, round(totalZ / heightPerEntity()));
				double textureRepeats = entities / (height() / heightPerEntity());
				height = totalZ / textureRepeats;
			}

			double x = v.x / width;
			double z = v.z / height;

			result.add(applyPadding(new VectorXZ(x, z)));

		}

		return result;

	}

	/** @see #scaleTexCoords(List, Double, Double) */
	public VectorXZ scaleTexCoords(VectorXZ rawTexCoords,
			@Nullable Double totalX, @Nullable Double totalZ) {
		return scaleTexCoords(List.of(rawTexCoords), totalX, totalZ).get(0);
	}

	/**
	 * modifies a calculated texture coordinate to account for {@link #padding()}.
	 * This is helpful when implementing {@link TexCoordFunction}s, not when using them.
	 */
	public VectorXZ applyPadding(VectorXZ texCoord) {
		if (padding == 0) {
			return texCoord;
		} else {
			return new VectorXZ(
					padding + texCoord.x * (1 - 2 * padding),
					padding + texCoord.z * (1 - 2 * padding)
			);
		}
	}

}
