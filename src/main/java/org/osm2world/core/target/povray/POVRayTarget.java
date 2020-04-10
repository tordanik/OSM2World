package org.osm2world.core.target.povray;

import java.awt.Color;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.AbstractTarget;
import org.osm2world.core.target.common.TextTextureData;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;

public class POVRayTarget extends AbstractTarget {

	private static final String INDENT = "  ";

	// this is approximatly one millimeter
	private static final double SMALL_OFFSET = 1e-3;

	private final PrintStream output;

	private Map<TextureData, String> textureNames = new HashMap<TextureData, String>();

	public POVRayTarget(PrintStream output) {
		this.output = output;
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

	public void appendMaterialDefinitions() {

		for (Material material : Materials.getMaterials()) {

			String uniqueName = Materials.getUniqueName(material);
			String name = "texture_" + uniqueName;

			append("#ifndef (" + name + ")\n");
			append("#declare " + name + " = ");
			appendMaterial(material);
			append("#end\n\n");

			if (material.getNumTextureLayers() == 1) {

				TextureData td = material.getTextureDataList().get(0);

				if (!td.colorable) {
					textureNames.put(td, uniqueName);
				}

			}

		}

	}

	@Override
	public void drawTriangles(Material material,
			Collection<? extends TriangleXYZ> triangles,
			List<List<VectorXZ>> texCoordLists) {

		if (!checkMeshValidity(triangles))
			return;

		for (TriangleXYZ triangle : triangles) {
			performNaNCheck(triangle);
		}

		if (material.getNumTextureLayers() > 1) {

			int count = 0;

			for (TextureData textureData : material.getTextureDataList()) {

				if(!(textureData instanceof TextTextureData)) { //temporarily ignore TextTextureData layers
					append("mesh {\n");

					drawTriangleMesh(triangles, texCoordLists.get(count), count);

					append("  uv_mapping ");
					appendMaterial(material, textureData);

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
	public void drawConvexPolygon(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists) {

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
	public void drawColumn(Material material, Integer corners, VectorXYZ base,
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

			super.drawColumn(material, corners, base, height, radiusBottom, radiusTop, drawBottom, drawTop);

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

		if (triangles.size() == 0)
			return false;

		boolean result = false;
		for (TriangleXYZ triangle : triangles) {

			result |= !isDegenerated(triangle);
		}

		return result;
	}

	private boolean isDegenerated(TriangleXYZ triangle) {

		VectorXYZ a = triangle.v1;
		VectorXYZ b = triangle.v2;
		VectorXYZ c = triangle.v3;

		if (a.equals(b) || a.equals(c) || b.equals(c)) {
			return true;
		} else if (a.x == b.x && b.x == c.x
				&& a.y == b.y && b.y == c.y) {
			return true;
		} else if (a.x == b.x && b.x == c.x
				&& a.z == b.z && b.z == c.z) {
			return true;
		} else if (a.y == b.y && b.y == c.y
				&& a.z == b.z && b.z == c.z) {
			return true;
		}
		return false;
	}


	public void appendTriangle(VectorXYZ a, VectorXYZ b, VectorXYZ c) {

		appendTriangle(a, b, c, null, null, null, false);
	}

	public void appendTriangle(
			VectorXYZ a, VectorXYZ b, VectorXYZ c,
			VectorXYZ na, VectorXYZ nb, VectorXYZ nc,
			boolean smooth) {

		appendTriangle(a, b, c, na, nb, nc, null, null, null, smooth, false);
	}

	public void appendTriangle(
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
	 * adds a color. Syntax is "color rgb <x, y, z>".
	 */
	public void appendRGBColor(Color color) {

		append("color rgb ");
		appendVector(
				color.getRed()/255f,
				color.getGreen()/255f,
				color.getBlue()/255f);

	}

	public void appendMaterialOrName(Material material) {

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
			append("      ambient " + material.getAmbientFactor() + "\n");
			append("      diffuse " + material.getDiffuseFactor() + "\n");
			append("    }\n");
			append("  }\n");

		} else {

			for (TextureData textureData : material.getTextureDataList()) {
				if(!(textureData instanceof TextTextureData)) { //temporarily ignore TextTextureData layers
					appendMaterial(material, textureData);
				}
			}
		}
	}


	private void appendMaterial(Material material, TextureData textureData) {

			String textureName = textureNames.get(textureData);

			if (textureName == null) {

				if (textureData.colorable) {
					append("  texture {\n");
					append("    pigment { ");
					appendRGBColor(material.getColor());
					append(" }\n    finish {\n");
					append("      ambient " + material.getAmbientFactor() + "\n");
					append("      diffuse " + material.getDiffuseFactor() + "\n");
					append("    }\n");
					append("  }\n");
				}

				append("  texture {\n");
				append("    pigment { ");
				appendImageMap(textureData);
				append(" }\n    finish {\n");
				append("      ambient " + material.getAmbientFactor() + "\n");
				append("      diffuse " + material.getDiffuseFactor() + "\n");
				append("    }\n");
				append("  }\n");

			} else {

				append("  texture { texture_" + textureName + "}");
			}
	}


	private void appendImageMap(TextureData textureData) {

			append("        image_map {\n");

			if (textureData.getRasterImage().getName().toLowerCase().endsWith("png")) {
				append("             png \"" + textureData.getRasterImage() + "\"\n");
			} else {
				append("             jpeg \"" + textureData.getRasterImage() + "\"\n");
			}

			if (textureData.colorable) {
				append("             filter all 1.0\n");
			}
			append("\n          }");
	}

	/**
	 * adds a vector to the String built by a StringBuilder.
	 * Syntax is "<x, y, z>".
	 */
	public void appendVector(float x, float y, float z) {

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

	public void appendVector(double x, double y, double z) {

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
	public void appendVector(VectorXYZ vector) {
		appendVector(vector.getX(), vector.getY(), vector.getZ());
	}

	/**
	 * adds a vector to the String built by a StringBuilder.
	 * Syntax is "<v1, v2>".
	 */
	public void appendVector(double x, double z) {

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
	public void appendVector(VectorXZ vector) {
		appendVector(vector.x, vector.z);
	}

	/**
	 * append a vector with inverted coordinates
	 */
	public void appendInverseVector(VectorXZ vector) {
		appendVector(-vector.x, -vector.z);
	}

	/**
	 *
	 * @param vs  polygon vertices; first and last should be equal
	 */
	public void appendPolygon(VectorXYZ... vs) {

		assert !vs[0].equals(vs[vs.length-1]) : "polygon not closed";

		append("polygon {\n  ");
		append(vs.length);
		append(", ");
		for (VectorXYZ v : vs) {
			appendVector(v);
		}
		append("}\n");

	}

	public void appendPrism(float y1, float y2, VectorXZ... vs) {

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

}

