package org.osm2world.output.povray;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.output.common.AbstractOutput;
import org.osm2world.output.common.DrawBasedOutput;
import org.osm2world.output.common.lighting.GlobalLightingParameters;
import org.osm2world.output.common.rendering.Camera;
import org.osm2world.output.common.rendering.OrthographicProjection;
import org.osm2world.output.common.rendering.Projection;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.*;
import org.osm2world.util.GlobalValues;

/**
 * Writes models to files for the POVRay ray tracer.
 */
public class POVRayOutput extends AbstractOutput implements DrawBasedOutput {

	protected static final float AMBIENT_FACTOR = 0.5f;

	private static final String INDENT = "  ";

	// this is approximately one millimeter
	private static final double SMALL_OFFSET = 1e-3;

	private final PrintStream output;

	private Map<TextureData, String> textureNames = new HashMap<TextureData, String>();

	protected @Nonnull O2WConfig config = new O2WConfig();

	public POVRayOutput(File file, Camera camera, Projection projection) throws FileNotFoundException {
		this(new PrintStream(file), camera, projection);
	}

	public POVRayOutput(PrintStream output, Camera camera, Projection projection) {

		this.output = output;

		appendCommentHeader();

		append("\n#include \"textures.inc\"\n#include \"colors.inc\"\n");
		append("#include \"osm2world_definitions.inc\"\n\n");

		if (camera != null && projection != null) {
			appendCameraDefinition(camera, projection);
		}

		append("//\n// global scene parameters\n//\n\n");

		appendLightingDefinition( GlobalLightingParameters.DEFAULT);

		appendDefaultParameterValue("season", "summer");
		appendDefaultParameterValue("time", "day");

		append("//\n// material and object definitions\n//\n\n");

		appendDefaultParameterValue("sky_sphere_def",
				"sky_sphere {\n pigment { Blue_Sky3 }\n} ");
		append("sky_sphere {sky_sphere_def}\n\n");

		appendMaterialDefinitions();

		//TODO get terrain boundary elsewhere
//		if (terrain != null) {
//
//			append("//\n// empty ground around the scene\n//\n\n");
//
//			append("difference {\n");
//			append("  plane { y, -0.001 }\n  ");
//			VectorXZ[] boundary = eleData.getBoundaryPolygon().getXZPolygon()
//				.getVertexLoop().toArray(new VectorXZ[0]);
//			appendPrism( -100, 1, boundary);
//			append("\n");
//			appendMaterialOrName(Materials.TERRAIN_DEFAULT);
//			append("\n}\n\n");
//
//		}

		append("\n\n//\n//Map data\n//\n\n");

	}

	@Override
	public O2WConfig getConfiguration() {
		return config;
	}

	@Override
	public void setConfiguration(O2WConfig config) {
		if (config != null) {
			this.config = config;
		} else {
			this.config = new O2WConfig();
		}
	}

	@Override
	public void finish() {
		output.close();
	}

	//	int openBrackets = 0;
//
//	/**
//	 * appends indentation based on {@link #INDENT} and {@link #openBrackets}
//	 */
//	private void appendIndent() {
//		for (int i=0; i<openBrackets; i++) {
//			append(INDENT);
//		}
//	}

	/**
	 * provides direct write access to the generated source code.
	 * This is intended for Renderables using special POVRay features.
	 */
	public void append(String code) {
		output.print(code);
//		if (code.contains("union") && openBrackets > 0) {
//			System.out.println(openBrackets);
//		}
//		for (int i=0; i<code.length(); i++) {
//			char c = code.charAt(i);
//			if (c == '{') {
//				openBrackets++;
//			} else if (c == '}') {
//				openBrackets--;
//			}
//		}
	}

	public void append(int value) {
		output.print(value);
	}

	public void append(double value) {
		output.print(value);
	}

//	private final LinkedList<StringBuilder> stack = new LinkedList<StringBuilder>();
//
//	int openBrackets = 0;
//
//	public void append(int value) {
//		stack.peek().append(value);
//	}
//
//	public void append(double value) {
//		stack.peek().append(value);
//	}
//
//	public void startBlock(String s) {
//		StringBuilder newBlock = new StringBuilder(s + "{");
//		stack.push(newBlock);
//	}
//
//	public void endBlock(String block) {
//		StringBuilder closedBlock = stack.poll();
//		if (stack.isEmpty()) {
//			output.append(closedBlock);
//		} else {
//			stack.peek().append(closedBlock);
//		}
//	}

	public void appendDefaultParameterValue(String name, String value) {

		append("#ifndef (" + name + ")\n");
		append("#declare " + name + " = " + value);
		append("\n#end\n\n");

	}

	private void appendCommentHeader() {

		append("/*\n"
				+ " * This file was created by OSM2World "
				+ GlobalValues.VERSION_STRING + " - "
				+ GlobalValues.OSM2WORLD_URI + "\n"
				+ " * \n"
				+ " * Make sure that a \"osm2world_definitions.inc\" file exists!\n"
				+ " * You can start with the one in the \"resources\" folder from your\n"
				+ " * OSM2World installation or even just create an empty file.\n"
				+ " */\n");

	}

	private void appendLightingDefinition(GlobalLightingParameters parameters) {

		append(String.format(Locale.ENGLISH,
				"global_settings { ambient_light rgb <%f,%f,%f> }\n",
				parameters.globalAmbientColor.getRed() / 255f,
				parameters.globalAmbientColor.getGreen() / 255f,
				parameters.globalAmbientColor.getBlue() / 255f));

		append(String.format(Locale.ENGLISH,
				"light_source{ <%f,%f,%f> color rgb <%f,%f,%f> parallel point_at <0,0,0> fade_power 0 }\n\n",
				parameters.lightFromDirection.x * 100000,
				parameters.lightFromDirection.y * 100000,
				parameters.lightFromDirection.z * 100000,
				parameters.lightColorDiffuse.getRed() / 255f,
				parameters.lightColorDiffuse.getGreen() / 255f,
				parameters.lightColorDiffuse.getBlue() / 255f));

	}

	private void appendCameraDefinition(Camera camera, Projection projection) {

		append("camera {");

		if (projection.orthographic()) {
			append("\n  orthographic");
		}

		append("\n  location ");
		appendVector(camera.pos());

		if (projection instanceof OrthographicProjection proj) {

			append("\n  right ");
			double width = proj.volumeWidth();
			appendVector(camera.getRight().mult(width).invert()); //invert compensates for left-handed vs. right handed coordinates

			append("\n  up ");
			VectorXYZ up = camera.up();
			appendVector(up.mult(proj.volumeHeight()));

			append("\n  look_at ");
			appendVector(camera.lookAt());

		} else {

			append("\n  look_at  ");
			appendVector(camera.lookAt());

			append("\n  sky ");
			appendVector(camera.up());

		}

		append("\n}\n\n");

	}

	private void appendMaterialDefinitions() {

		for (Material material : Materials.getMaterials()) {

			String uniqueName = Materials.getUniqueName(material);
			String name = "texture_" + uniqueName;

			append("#ifndef (" + name + ")\n");
			append("#declare " + name + " = ");
			appendMaterial(material);
			append("#end\n\n");

			if (material.getNumTextureLayers() == 1) {

				TextureLayer layer = material.getTextureLayers().get(0);
				TextureData td = layer.baseColorTexture;

				if (!layer.colorable) {
					textureNames.put(td, uniqueName);
				}

			}

		}

	}

	@Override
	public void drawTriangles(@Nonnull Material material,
							  @Nonnull List<? extends TriangleXYZ> triangles,
							  @Nonnull List<List<VectorXZ>> texCoordLists) {

		if (!checkMeshValidity(triangles))
			return;

		for (TriangleXYZ triangle : triangles) {
			performNaNCheck(triangle);
		}

		if (material.getNumTextureLayers() > 1) {

			int count = 0;

			for (TextureLayer textureLayer : material.getTextureLayers()) {

				if(!(textureLayer.baseColorTexture instanceof TextTexture)) { //temporarily ignore TextTextureData layers
					append("mesh {\n");

					drawTriangleMesh(triangles, texCoordLists.get(count), count);

					append("  uv_mapping ");
					appendMaterial(material, textureLayer.baseColorTexture, textureLayer.colorable);

					if (count > 0)
						append("  no_shadow");
					append("}\n");
					count++;
				}
			}
		} else {

				append("mesh {\n");

				if (texCoordLists.size() > 0) {
					drawTriangleMesh(triangles, texCoordLists.get(0), 0);
				} else {
					for (TriangleXYZ triangle : triangles) {
						append(INDENT);
						appendTriangle(triangle.v1, triangle.v2, triangle.v3);
					}
				}

				append(" uv_mapping ");
				appendMaterialOrName(material);

				append("}\n");

		}
	}

	private void drawTriangleMesh(Collection<? extends TriangleXYZ> triangles,
			List<VectorXZ> texCoordList, int depth) {

		Iterator<? extends TriangleXYZ> itr1 = triangles.iterator();
		Iterator<VectorXZ> itr2 = texCoordList.iterator();

		while (itr1.hasNext()) {

			TriangleXYZ triangle = itr1.next();
			VectorXYZ normal = triangle.getNormal();
			VectorXZ tex1 = itr2.next();
			VectorXZ tex2 = itr2.next();
			VectorXZ tex3 = itr2.next();

			append(INDENT);

			if (depth > 0) {

				normal = normal.mult(depth*SMALL_OFFSET);
				appendTriangle(
						triangle.v1.add(normal),
						triangle.v2.add(normal),
						triangle.v3.add(normal),
						null, null, null, tex1, tex2, tex3, false, true);

			} else {

				appendTriangle(triangle.v1, triangle.v2, triangle.v3,
						null, null, null, tex1, tex2, tex3, false, true);
			}
		}
	}

//	@Override
//	public void drawTriangleStrip(Material material, VectorXYZ... vectors) {
//
//		for (VectorXYZ vector : vectors) {
//			performNaNCheck(vector);
//		}
//
//		append("union {\n");
//
//		for (int triangle = 0; triangle + 2 < vectors.length; triangle++) {
//
//			append(INDENT);
//
//			appendTriangle(
//					vectors[triangle],
//					vectors[triangle + 1],
//					vectors[triangle + 2]);
//
//		}
//
//		appendMaterial(material);
//
//		append("}\n");
//
//	}

//	@Override
//	public void drawTriangleFan(Material material, List<? extends VectorXYZ> vs) {
//		for (VectorXYZ vector : vs) {
//			performNaNCheck(vector);
//		}
//
//		append("union {\n");
//
//		VectorXYZ center = vs.get(0);
//
//		for (int triangle = 0; triangle + 2 < vs.size(); triangle ++) {
//
//			append(INDENT);
//
//			appendTriangle(
//					center,
//					vs.get(triangle + 1),
//					vs.get(triangle + 2));
//
//		}
//
//		appendMaterial(material);
//
//		append("}\n");
//
//	}

	@Override
	public void drawConvexPolygon(@Nonnull Material material, @Nonnull List<VectorXYZ> vs,
								  @Nonnull List<List<VectorXZ>> texCoordLists) {

		for (VectorXYZ vector : vs) {
			performNaNCheck(vector);
		}

		append("polygon {\n  ");
		append(vs.size());
		append(", ");
		for (VectorXYZ v : vs) {
			appendVector(v);
		}

		appendMaterialOrName(material);

		append("}\n");

	}

	@Override
	public void drawColumn(@Nonnull Material material, Integer corners, @Nonnull VectorXYZ base,
						   double height, double radiusBottom, double radiusTop,
						   boolean drawBottom, boolean drawTop) {

		performNaNCheck(base);

		if (height <= 0) return;

		if (corners == null) {

			if (radiusBottom == radiusTop) { // cylinder

				append("cylinder {\n  ");
				appendVector(base);
				append(", ");
				appendVector(base.y(base.y + height));
				append(", ");
				append(radiusTop);

			} else { // (truncated) cone

				append("cone {\n  ");
				appendVector(base);
				append(", "); append(radiusBottom); append(", ");
				appendVector(base.y(base.y + height));
				append(", "); append(radiusTop);

			}

			if (!drawBottom && !drawTop) {
				// TODO: incorrect if only one is false
				append(" open");
			}

			appendMaterialOrName(material);

			append("}\n");

		} else { // not round

			DrawBasedOutput.super.drawColumn(material, corners, base, height, radiusBottom, radiusTop, drawBottom, drawTop);

		}

	}

	/**
	 * variant of {@link #drawColumn(Material, Integer, VectorXYZ, double, double, double, boolean, boolean)}
	 * that allows arbitrarily placed columns
	 */
	public void drawColumn(Material material, Integer corners, VectorXYZ base,
			VectorXYZ cap, double radiusBottom, double radiusTop,
			boolean drawBottom, boolean drawTop) {

		performNaNCheck(base);
		performNaNCheck(cap);

		if (cap.equals(base))
			return;

		if (corners == null) {

			if (radiusBottom == radiusTop) { // cylinder

				append("cylinder {\n  ");
				appendVector(base);
				append(", ");
				appendVector(cap);
				append(", "); append(radiusTop);

			} else { // (truncated) cone

				append("cone {\n  ");
				appendVector(base);
				append(", "); append(radiusBottom); append(", ");
				appendVector(cap);
				append(", "); append(radiusTop);

			}

			if (!drawBottom && !drawTop) {
				// TODO: incorrect if only one is false
				append(" open");
			}

		} else { // not round

			throw new UnsupportedOperationException(
					"drawing non-round columns isn't implemented yet");

		}

		appendMaterialOrName(material);

		append("}\n");

	}

	private boolean checkMeshValidity(Collection<? extends TriangleXYZ> triangles) {
		return (triangles.size() >= 0);
	}

	private void appendTriangle(VectorXYZ a, VectorXYZ b, VectorXYZ c) {

		appendTriangle(a, b, c, null, null, null, false);
	}

	private void appendTriangle(
			VectorXYZ a, VectorXYZ b, VectorXYZ c,
			VectorXYZ na, VectorXYZ nb, VectorXYZ nc,
			boolean smooth) {

		appendTriangle(a, b, c, na, nb, nc, null, null, null, smooth, false);
	}

	private void appendTriangle(
			VectorXYZ a, VectorXYZ b, VectorXYZ c,
			VectorXYZ na, VectorXYZ nb, VectorXYZ nc,
			VectorXZ ta, VectorXZ tb, VectorXZ tc,
			boolean smooth, boolean texture) {

		// append the triangle

		if (smooth) append("smooth_");
		append("triangle { ");
		appendVector(a);
		if (smooth) {
			append(", ");
			appendVector(na);
		}
		append(", ");
		appendVector(b);
		if (smooth) {
			append(", ");
			appendVector(nb);
		}
		append(", ");
		appendVector(c);
		if (smooth) {
			append(", ");
			appendVector(nc);
		}

		if (texture) {
			/*
			append(" uv_vectors ");
			appendInverseVector(ta);
			append(", ");
			appendInverseVector(tb);
			append(", ");
			appendInverseVector(tc);
			*/

			append(" uv_vectors ");
			appendVector(ta);
			append(", ");
			appendVector(tb);
			append(", ");
			appendVector(tc);
		}

		append("}\n");
	}


	/**
	 * adds a color. Syntax is "color rgb &lt;x, y, z&gt;".
	 */
	private void appendRGBColor(Color color) {

		append("color rgb ");
		appendVector(
				color.getRed()/255f,
				color.getGreen()/255f,
				color.getBlue()/255f);

	}

	private void appendMaterialOrName(Material material) {

		String materialName = Materials.getUniqueName(material);

		if (materialName != null) {
			append(" texture { texture_" + materialName + " }");
		} else {
			appendMaterial(material);
		}

	}

	private void appendMaterial(Material material) {

		if (material.getNumTextureLayers() == 0) {

			append("  texture {\n");
			append("    pigment { ");
			appendRGBColor(material.getColor());
			append(" }\n    finish {\n");
			append("      ambient " + AMBIENT_FACTOR + "\n");
			append("      diffuse " + (1 - AMBIENT_FACTOR) + "\n");
			append("    }\n");
			append("  }\n");

		} else {

			for (TextureLayer textureLayer : material.getTextureLayers()) {
				if(!(textureLayer.baseColorTexture instanceof TextTexture)) { //temporarily ignore TextTextureData layers
					appendMaterial(material, textureLayer.baseColorTexture, textureLayer.colorable);
				}
			}
		}
	}


	private void appendMaterial(Material material, TextureData textureData, boolean colorable) {

			String textureName = textureNames.get(textureData);

			if (textureName == null) {

				if (colorable) {
					append("  texture {\n");
					append("    pigment { ");
					appendRGBColor(material.getColor());
					append(" }\n    finish {\n");
					append("      ambient " + AMBIENT_FACTOR + "\n");
					append("      diffuse " + (1 - AMBIENT_FACTOR) + "\n");
					append("    }\n");
					append("  }\n");
				}

				append("  texture {\n");
				append("    pigment { ");
				appendImageMap(textureData, colorable);
				append(" }\n    finish {\n");
				append("      ambient " + AMBIENT_FACTOR + "\n");
				append("      diffuse " + (1 - AMBIENT_FACTOR) + "\n");
				append("    }\n");
				append("  }\n");

			} else {

				append("  texture { texture_" + textureName + "}");
			}
	}


	private void appendImageMap(TextureData textureData, boolean colorable) {

			append("        image_map {\n");

			try {
				File textureFile = getTextureFile(textureData);
				if (textureFile.getName().toLowerCase().endsWith("png")) {
					append("             png \"" + textureFile + "\"\n");
				} else {
					append("             jpeg \"" + textureFile + "\"\n");
				}

				if (colorable) {
					append("             filter all 1.0\n");
				}
			} catch (IOException e) {
				System.err.println("Could not append image_map for texture " + textureData + ":" + e);
			}

			append("\n          }");
	}

	/**
	 * adds a vector to the String built by a StringBuilder.
	 * Syntax is "&lt;x, y, z&gt;".
	 */
	private void appendVector(float x, float y, float z) {

		if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)) {
			throw new IllegalArgumentException("NaN vector " + x + ", " + y + ", " + z);
		}

		append("<");
		append(x);
		append(", ");
		append(y);
		append(", ");
		append(z);
		append(">");

	}

	private void appendVector(double x, double y, double z) {

		if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
			throw new IllegalArgumentException("NaN vector " + x + ", " + y + ", " + z);
		}

		append("<");
		append(x);
		append(", ");
		append(y);
		append(", ");
		append(z);
		append(">");

	}

	/**
	 * alternative to {@link #appendVector(double, double)}
	 * using a vector object as parameter instead of individual coordinates
	 */
	private void appendVector(VectorXYZ vector) {
		appendVector(vector.getX(), vector.getY(), vector.getZ());
	}

	/**
	 * adds a vector to the String built by a StringBuilder.
	 * Syntax is "&lt;v1, v2&gt;".
	 */
	private void appendVector(double x, double z) {

		append("<");
		append(x);
		append(", ");
		append(z);
		append(">");

	}

	/**
	 * alternative to {@link #appendVector(double, double)}
	 * using a vector object as parameter instead of individual coordinates
	 */
	private void appendVector(VectorXZ vector) {
		appendVector(vector.x, vector.z);
	}

	/**
	 * append a vector with inverted coordinates
	 */
	private void appendInverseVector(VectorXZ vector) {
		appendVector(-vector.x, -vector.z);
	}

	/**
	 *
	 * @param vs  polygon vertices; first and last should be equal
	 */
	private void appendPolygon(VectorXYZ... vs) {

		assert !vs[0].equals(vs[vs.length-1]) : "polygon not closed";

		append("polygon {\n  ");
		append(vs.length);
		append(", ");
		for (VectorXYZ v : vs) {
			appendVector(v);
		}
		append("}\n");

	}

	private void appendPrism(float y1, float y2, VectorXZ... vs) {

		append("prism {\n  ");
		append(y1);
		append(", ");
		append(y2);
		append(", ");
		append(vs.length);
		append(",\n  ");
		for (VectorXZ v : vs) {
			appendVector(v);
		}
		append("\n}");

	}

	//TODO: avoid having to do this
	private void performNaNCheck(TriangleXYZ triangle) {
		performNaNCheck(triangle.v1);
		performNaNCheck(triangle.v2);
		performNaNCheck(triangle.v3);
	}

	private void performNaNCheck(VectorXYZ v) {
		if (Double.isNaN(v.x) || Double.isNaN(v.y) || Double.isNaN(v.z)) {
			throw new IllegalArgumentException("NaN vector " + v.x + ", " + v.y + ", " + v.z);
		}
	}

	private final Map<TextureData, File> textureFileMap = new HashMap<>();

	private File getTextureFile(TextureData texture) throws IOException {

		if (!textureFileMap.containsKey(texture)) {

			BufferedImage image = texture.getBufferedImage();
			String prefix = "o2w-";

			File textureFile = File.createTempFile(prefix, ".png");
			ImageIO.write(image, "png", textureFile);

			textureFileMap.put(texture, textureFile);

		}

		return textureFileMap.get(texture);


	}

}

