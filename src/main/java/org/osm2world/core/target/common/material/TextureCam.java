package org.osm2world.core.target.common.material;

import static java.lang.Math.*;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.math.NumberUtils.min;
import static org.osm2world.core.math.GeometryUtil.interpolateOnTriangle;
import static org.osm2world.core.math.VectorXYZ.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.TextureData.Wrap;
import org.osm2world.core.target.common.material.TextureLayer.TextureType;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.mesh.TriangleGeometry;
import org.osm2world.core.target.common.texcoord.NamedTexCoordFunction;
import org.osm2world.core.target.common.texcoord.TexCoordFunction;
import org.osm2world.core.util.Resolution;
import org.osm2world.core.util.color.LColor;

/**
 * A "texture camera" that produces textures by rendering a scene from a given camera position.
 * Produces all types of textures, not just color textures, i.e. it attempts to encode the geometry with normals etc.
 */
public class TextureCam {

	private TextureCam() { }

	public static enum ViewDirection { FROM_FRONT, FROM_TOP }

	public static final Resolution DEFAULT_RESOLUTION = new Resolution(1024, 1024);

	private static final int TRANSPARENT_RGB = new Color(0, 0, 0, 0).getRGB();

	public static final TextureLayer renderTextures(List<Mesh> meshes, ViewDirection viewDirection, String name,
			double width, double height, Double widthPerEntity, Double heightPerEntity, Wrap wrap,
			@Nullable VectorXYZ center) {

		Map<TriangleXZ, Double> heightMap = new HashMap<>();
		Map<TriangleXZ, Material> materialMap = new HashMap<>();
		Map<TriangleXZ, List<List<VectorXZ>>> texCoordMap = new HashMap<>();
		//TODO: add maps for normals and vertex colors

		for (Mesh mesh : meshes) {

			TriangleGeometry tg = mesh.geometry.asTriangles();

			for (int i = 0; i < tg.triangles.size(); i++) {

				TriangleXYZ t = tg.triangles.get(i);

				if (center != null) {
					t = t.shift(center.invert());
				}

				if (viewDirection == ViewDirection.FROM_FRONT) {
					t = new TriangleXYZ(
							t.v1.rotateVec(PI / 2, NULL_VECTOR, X_UNIT),
							t.v2.rotateVec(PI / 2, NULL_VECTOR, X_UNIT),
							t.v3.rotateVec(PI / 2, NULL_VECTOR, X_UNIT));
				}

				TriangleXZ tXZ = new TriangleXZ(t.v1.xz(), t.v2.xz(), t.v3.xz());

				if (!tXZ.isDegenerateOrNaN()) {

					materialMap.put(tXZ, mesh.material);
					heightMap.put(tXZ, t.getCenter().y); // FIXME using the center doesn't work in less simple cases

					List<List<VectorXZ>> texCoords = new ArrayList<>();
					for (int layer = 0; layer < tg.texCoords.size(); layer++) {
						texCoords.add(asList(
								tg.texCoords.get(layer).get(3 * i),
								tg.texCoords.get(layer).get(3 * i + 1),
								tg.texCoords.get(layer).get(3 * i + 2)));
					}

					texCoordMap.put(tXZ, texCoords);

				}

			}

		}

		/* produce the images by finding the closest triangle at any point */

		Resolution res = DEFAULT_RESOLUTION;

		BufferedImage colorImage = new BufferedImage(res.width, res.height, BufferedImage.TYPE_INT_ARGB);
		BufferedImage normalImage = new BufferedImage(res.width, res.height, BufferedImage.TYPE_INT_RGB);
		BufferedImage ormImage = new BufferedImage(res.width, res.height, BufferedImage.TYPE_INT_RGB);
		BufferedImage displacementImage = new BufferedImage(res.width, res.height, BufferedImage.TYPE_INT_RGB);

		List<TriangleXZ> intersectedTriangles = new ArrayList<>();

		for (int y = 0; y < res.height; y++) {
			for (int x = 0; x < res.width; x++) {

				VectorXZ point = new VectorXZ(
						width * ((x / (double)res.width) - 0.5),
						height * ((y / (double)res.height) - 0.5));

				intersectedTriangles.clear();

				for (TriangleXZ t : heightMap.keySet()) {
					if (t.contains(point)) {
						intersectedTriangles.add(t);
					}
				}

				Optional<TriangleXZ> optionalT = intersectedTriangles.stream().max(Comparator.comparingDouble(heightMap::get));

				if (!optionalT.isPresent()) {
					colorImage.setRGB(x, y, TRANSPARENT_RGB);
				} else {
					TriangleXZ t = optionalT.get();
					Material m = materialMap.get(t);
					VectorXZ texCoord = null;
					if (texCoordMap.size() > 0) {
						texCoord = interpolateOnTriangle(point, t, texCoordMap.get(t).get(0).get(0),
								texCoordMap.get(t).get(0).get(1), texCoordMap.get(t).get(0).get(2));
					}
					LColor c = pickColorAtTexCoord(TextureType.BASE_COLOR, texCoord, m);
					colorImage.setRGB(x, y, c.toAWT().getRGB());
				}

			}
		}

		/* build and return the result */

		return new TextureLayer(
				new RenderedTexture(colorImage, name + " color",
						width, height, widthPerEntity, heightPerEntity, wrap, NamedTexCoordFunction.GLOBAL_X_Z),
				new RenderedTexture(normalImage, name + " normal",
						width, height, widthPerEntity, heightPerEntity, wrap, NamedTexCoordFunction.GLOBAL_X_Z),
				new RenderedTexture(ormImage, name + " orm",
						width, height, widthPerEntity, heightPerEntity, wrap, NamedTexCoordFunction.GLOBAL_X_Z),
				new RenderedTexture(displacementImage, name + " displacement",
						width, height, widthPerEntity, heightPerEntity, wrap, NamedTexCoordFunction.GLOBAL_X_Z),
				false);

	}

	private static LColor pickColorAtTexCoord(TextureType textureType, @Nullable VectorXZ texCoord, Material material) {

		LColor c = LColor.fromAWT(material.color);

		if (material.getNumTextureLayers() > 0) { // TODO support multiple texture layers

			TextureData texture = material.getTextureLayers().get(0).getTexture(textureType);

			if (texture != null && texCoord != null) {

				double texX = texCoord.x % 1;
				double texZ = texCoord.z % 1;

				while (texX < 0) texX += 1.0;
				while (texZ < 0) texZ += 1.0;

				BufferedImage image = texture.getBufferedImage();
				int x = min((int)floor(image.getWidth() * texX), image.getWidth() - 1);
				int y = min((int)floor(image.getHeight() * texZ), image.getHeight() - 1);
				LColor textureColor = LColor.fromAWT(new Color(image.getRGB(x, y)));

				if (material.getTextureLayers().get(0).colorable) {
					c = c.multiply(textureColor);
				} else {
					c = textureColor;
				}

			}

		}

		return c;

	}

	private static final class RenderedTexture extends RuntimeTexture {

		private final BufferedImage image;
		private final String name;

		public RenderedTexture(BufferedImage image, String name, double width, double height, Double widthPerEntity,
				Double heightPerEntity, Wrap wrap, Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
			super(width, height, widthPerEntity, heightPerEntity, wrap, texCoordFunction);
			this.image = image;
			this.name = name;
		}

		@Override
		protected BufferedImage createBufferedImage() {
			return image;
		}

		@Override
		public String toString() {
			return name;
		}

	}

}
