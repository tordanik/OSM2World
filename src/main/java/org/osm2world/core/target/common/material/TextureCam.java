package org.osm2world.core.target.common.material;

import static java.lang.Double.isFinite;
import static java.lang.Math.PI;
import static java.util.Arrays.*;
import static java.util.Collections.nCopies;
import static org.osm2world.core.math.GeometryUtil.interpolateOnTriangle;
import static org.osm2world.core.math.VectorXYZ.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

	private static class TriangleWithAttributes {

		public final TriangleXZ triangle;
		public final double height;
		public final List<Double> vertexHeights;
		public final Material material;
		public final List<List<VectorXZ>> texCoords;
		public final List<VectorXYZ> normals;
		public final List<LColor> colors;

		public TriangleWithAttributes(TriangleXZ triangle, double height, List<Double> vertexHeights, Material material,
				List<List<VectorXZ>> texCoords, List<VectorXYZ> normals, List<LColor> colors) {
			this.triangle = triangle;
			this.height = height;
			this.vertexHeights = vertexHeights;
			this.material = material;
			this.texCoords = texCoords;
			this.normals = normals;
			this.colors = colors;
		}

	}

	public static final TextureLayer renderTextures(List<Mesh> meshes, ViewDirection viewDirection, String name,
			double width, double height, Double widthPerEntity, Double heightPerEntity, Wrap wrap,
			@Nullable VectorXYZ center) {

		List<TriangleWithAttributes> triangles = new ArrayList<>();

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

					double tHeight = t.getCenter().y; // FIXME using the center doesn't work in less simple cases

					List<Double> vertexHeights = asList(t.v1.y, t.v2.y, t.v3.y);

					List<VectorXYZ> normals = asList(
							tg.normalData.normals().get(3 * i),
							tg.normalData.normals().get(3 * i + 1),
							tg.normalData.normals().get(3 * i + 2));

					List<LColor> colors = null;

					if (tg.colors != null) {
						colors = asList(
								LColor.fromAWT(tg.colors.get(3 * i)),
								LColor.fromAWT(tg.colors.get(3 * i + 1)),
								LColor.fromAWT(tg.colors.get(3 * i + 2)));
					} else if (mesh.material.getNumTextureLayers() == 0
							|| mesh.material.getTextureLayers().get(0).colorable) {
						colors = nCopies(3, LColor.fromAWT(mesh.material.color));
					} else {
						colors = nCopies(3, LColor.WHITE);
					}

					List<List<VectorXZ>> texCoords = new ArrayList<>();
					for (int layer = 0; layer < tg.texCoords.size(); layer++) {
						texCoords.add(asList(
								tg.texCoords.get(layer).get(3 * i),
								tg.texCoords.get(layer).get(3 * i + 1),
								tg.texCoords.get(layer).get(3 * i + 2)));
					}

					triangles.add(new TriangleWithAttributes(tXZ, tHeight, vertexHeights, mesh.material,
							texCoords, normals, colors));

				}

			}

		}

		/* produce the images by finding the closest triangle at any point */

		Resolution res = DEFAULT_RESOLUTION;

		BufferedImage colorImage = new BufferedImage(res.width, res.height, BufferedImage.TYPE_INT_ARGB);
		BufferedImage normalImage = new BufferedImage(res.width, res.height, BufferedImage.TYPE_INT_RGB);
		BufferedImage ormImage = new BufferedImage(res.width, res.height, BufferedImage.TYPE_INT_RGB);
		Double[][] displacementHeights = new Double[res.width][res.height];

		List<TriangleWithAttributes> intersectedTriangles = new ArrayList<>();

		for (int y = 0; y < res.height; y++) {
			for (int x = 0; x < res.width; x++) {

				VectorXZ point = new VectorXZ(
						width * ((x / (double)res.width) - 0.5),
						height * ((y / (double)res.height) - 0.5) * -1);

				intersectedTriangles.clear();

				for (TriangleWithAttributes t : triangles) {
					if (t.triangle.contains(point)) {
						intersectedTriangles.add(t);
					}
				}

				Optional<TriangleWithAttributes> optionalT = intersectedTriangles.stream()
						.max(Comparator.comparingDouble(t -> t.height));

				if (!optionalT.isPresent()) {

					colorImage.setRGB(x, y, TRANSPARENT_RGB);

				} else {

					TriangleWithAttributes t = optionalT.get();
					VectorXZ texCoord = null;
					if (t.texCoords.size() > 0) {
						texCoord = interpolateOnTriangle(point, t.triangle, t.texCoords.get(0).get(0),
								t.texCoords.get(0).get(1), t.texCoords.get(0).get(2));
					}

					LColor geometryColor = interpolateOnTriangle(point,
							t.triangle, t.colors.get(0), t.colors.get(1), t.colors.get(2));
					LColor c = pickColorAtTexCoord(TextureType.BASE_COLOR, texCoord, t.material, geometryColor);
					colorImage.setRGB(x, y, c.toAWT().getRGB());

					LColor cOrm = pickColorAtTexCoord(TextureType.ORM, texCoord, t.material, LColor.WHITE);
					ormImage.setRGB(x, y, cOrm.toAWT().getRGB());

					// LColor cNormal = pickColorAtTexCoord(TextureType.ORM, texCoord, m);
					// TODO: extract normal vector, rotate it based on interpolated normal (NOT triangle normal), convert back to color
					// start implementation by JUST interpolating triangle normal

					displacementHeights[x][y] = interpolateOnTriangle(point, t.triangle, t.vertexHeights.get(0),
							t.vertexHeights.get(1), t.vertexHeights.get(2));
					// TODO: add displacement from existing displacement textures

				}

			}
		}

		/* create the displacement texture (needs min and max value, therefore after the loop) */

		BufferedImage displacementImage = null;

		double minHeight = stream(displacementHeights)
				.flatMap(it -> stream(it))
				.mapToDouble(it -> it != null ? it : Double.POSITIVE_INFINITY)
				.min().getAsDouble();

		double maxHeight = stream(displacementHeights)
				.flatMap(it -> stream(it))
				.mapToDouble(it -> it != null ? it : Double.NEGATIVE_INFINITY)
				.max().getAsDouble();

		if (isFinite(minHeight) && isFinite(maxHeight)) {

			displacementImage = new BufferedImage(res.width, res.height, BufferedImage.TYPE_INT_RGB);

			for (int y = 0; y < res.height; y++) {
				for (int x = 0; x < res.width; x++) {

					double absoluteHeight = displacementHeights[x][y] != null ? displacementHeights[x][y] : minHeight;

					float value = (float) ((absoluteHeight - minHeight) / (maxHeight - minHeight));

					LColor cDisplacement = new LColor(value, value, value);
					displacementImage.setRGB(x, y, cDisplacement.toAWT().getRGB());

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
				displacementImage == null ? null : new RenderedTexture(displacementImage, name + " displacement",
						width, height, widthPerEntity, heightPerEntity, wrap, NamedTexCoordFunction.GLOBAL_X_Z),
				false);

	}

	private static LColor pickColorAtTexCoord(TextureType textureType, @Nullable VectorXZ texCoord, Material material,
			LColor baseColor) {

		LColor c = baseColor;

		if (material.getNumTextureLayers() > 0) { // TODO support multiple texture layers

			TextureData texture = material.getTextureLayers().get(0).getTexture(textureType);

			if (texture != null && texCoord != null) {

				LColor textureColor = texture.getColorAt(texCoord);

				if (textureType == TextureType.BASE_COLOR && material.getTextureLayers().get(0).colorable) {
					c = c.multiply(textureColor);
				} else {
					c = textureColor;
				}

			}

		}

		return c;

	}

	private static VectorXYZ normalFromColor(LColor color) {

		return NULL_VECTOR; //FIXME implement

	}

	private static LColor colorFromNormal(VectorXYZ color) {
		return new LColor(1, 0, 0); //FIXME implement
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
