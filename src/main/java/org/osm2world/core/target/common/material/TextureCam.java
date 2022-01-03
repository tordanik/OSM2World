package org.osm2world.core.target.common.material;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.TextureData.Wrap;
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
			double width, double height, Double widthPerEntity, Double heightPerEntity, Wrap wrap) {

		Map<TriangleXZ, Double> heightMap = new HashMap<>();
		Map<TriangleXZ, Material> materialMap = new HashMap<>();
		Map<TriangleXZ, List<VectorXZ>> texCoordMap = new HashMap<>(); //TODO: use this, add normals and vertex colors

		for (Mesh mesh : meshes) {

			TriangleGeometry tg = mesh.geometry.asTriangles();

			for (int i = 0; i < tg.triangles.size(); i++) {

				TriangleXYZ t = tg.triangles.get(i);

				if (viewDirection == ViewDirection.FROM_FRONT) {
					// TODO v.rotateVec(PI, NULL_VECTOR, Z_UNIT);
				}

				TriangleXZ tXZ = new TriangleXZ(t.v1.xz(), t.v2.xz(), t.v3.xz());

				if (!tXZ.isDegenerateOrNaN()) {
					materialMap.put(tXZ, mesh.material);
					heightMap.put(tXZ, t.getCenter().y); // FIXME using the center doesn't work in less simple cases
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

				Optional<TriangleXZ> t = intersectedTriangles.stream().max(Comparator.comparingDouble(heightMap::get));

				if (!t.isPresent()) {
					colorImage.setRGB(x, y, TRANSPARENT_RGB);
				} else {
					Material m = materialMap.get(t.get());
					LColor c = LColor.fromAWT(m.color);
					if (m.getNumTextureLayers() > 0) {
						c = m.getTextureLayers().get(0).baseColorTexture.getAverageColor();
						// TODO use texture coordinates to pick the color in that location, not just the average one
					}
					// TODO multiply material color, vertex colors (if any) and layer color
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
