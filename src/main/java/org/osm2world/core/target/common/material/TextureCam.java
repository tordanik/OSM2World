package org.osm2world.core.target.common.material;

import static java.lang.Double.isFinite;
import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.nCopies;
import static org.osm2world.core.math.GeometryUtil.interpolateOnTriangle;
import static org.osm2world.core.math.VectorXYZ.*;

import java.awt.*;
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
	private static final int DEFAULT_NORMAL_RGB = new LColor(0.5f, 0.5f, 1f).toAWT().getRGB();

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
			TextureDataDimensions dimensions, Wrap wrap, @Nullable VectorXYZ center, double displacementScale) {

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
						dimensions.width() * ((x / (double)res.width) - 0.5),
						dimensions.height() * ((y / (double)res.height) - 0.5) * -1);

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
					normalImage.setRGB(x, y, DEFAULT_NORMAL_RGB);

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

					LColor cNormal = pickColorAtTexCoord(TextureType.NORMAL, texCoord, t.material, LColor.WHITE);
					if (cNormal == LColor.WHITE) {
						cNormal = colorFromNormal(Z_UNIT);
					}
					VectorXYZ geometryNormal = interpolateOnTriangle(point, t.triangle,
							t.normals.get(0), t.normals.get(1), t.normals.get(2));
					geometryNormal = new VectorXYZ(geometryNormal.x, geometryNormal.y, -geometryNormal.z);
					if (viewDirection == ViewDirection.FROM_TOP) {
						geometryNormal = geometryNormal.rotateVec(PI / 2, NULL_VECTOR, X_UNIT);
					}
					if (geometryNormal.distanceTo(Z_UNIT) > 0.1) {
						// overwrite texture normal with geometry normal unless it's almost directly facing the camera
						// TODO: general solution: rotate texture normal vector based on geometry normal vector (MikkTSpace)
						cNormal = colorFromNormal(geometryNormal);
					}
					normalImage.setRGB(x, y, cNormal.toAWT().getRGB());

					displacementHeights[x][y] = interpolateOnTriangle(point, t.triangle, t.vertexHeights.get(0),
							t.vertexHeights.get(1), t.vertexHeights.get(2));
					if (geometryNormal.distanceTo(Z_UNIT) <= 0.1) {
						// add displacement from existing displacement textures
						// TODO: do this even if the geometry is not (almost) directly facing the camera
						double origDisplacement = pickColorAtTexCoord(TextureType.DISPLACEMENT,
								texCoord, t.material, LColor.WHITE).red;
						displacementHeights[x][y] += origDisplacement * displacementScale;
					}

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
						dimensions, wrap, NamedTexCoordFunction.GLOBAL_X_Z),
				new RenderedTexture(normalImage, name + " normal",
						dimensions, wrap, NamedTexCoordFunction.GLOBAL_X_Z),
				new RenderedTexture(ormImage, name + " orm",
						dimensions, wrap, NamedTexCoordFunction.GLOBAL_X_Z),
				displacementImage == null ? null : new RenderedTexture(displacementImage, name + " displacement",
						dimensions, wrap, NamedTexCoordFunction.GLOBAL_X_Z),
				false);

	}

	private static LColor pickColorAtTexCoord(TextureType textureType, @Nullable VectorXZ texCoord, Material material,
			LColor baseColor) {

		LColor c = baseColor;

		if (material.getNumTextureLayers() > 0) { // TODO support multiple texture layers

			TextureData texture = material.getTextureLayers().get(0).getTexture(textureType);

			if (texture != null && texCoord != null) {

				LColor textureColor = texture.getColorAt(texCoord, Wrap.REPEAT);

				if (textureType == TextureType.BASE_COLOR && material.getTextureLayers().get(0).colorable) {
					c = c.multiply(textureColor);
				} else {
					c = textureColor;
				}

			}

		}

		return c;

	}

	/**
	 * represents a normal vector as a color value for use in normal textures.
	 * Follows §3.9.3 of the glTF 2.0 specification.
	 */
	static VectorXYZ normalFromColor(LColor color) {
		assert color.blue > 0.5;
		return new VectorXYZ(
				-1.0 + 2 * color.red,
				-1.0 + 2 * color.green,
				-1.0 + 2 * color.blue);
	}

	/** inverse of {@link #normalFromColor(LColor)} */
	static LColor colorFromNormal(VectorXYZ normal) {
		assert normal.x >= -1.0 && normal.x <= 1.0
				&& normal.y >= -1.0 && normal.y <= 1.0
				&& normal.z > 0.0 && normal.z <= 1.0;
		return new LColor(
				(float)(normal.x + 1.0) / 2,
				(float)(normal.y + 1.0) / 2,
				(float)(normal.z + 1.0) / 2);
	}

	private static final class RenderedTexture extends RuntimeTexture {

		private final BufferedImage image;
		private final String name;

		public RenderedTexture(BufferedImage image, String name, TextureDataDimensions dimensions, Wrap wrap,
							   Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
			super(dimensions, wrap, texCoordFunction);
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
