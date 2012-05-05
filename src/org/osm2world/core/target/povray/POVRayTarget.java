package org.osm2world.core.target.povray;

import java.awt.Color;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.AbstractTarget;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;

public class POVRayTarget extends AbstractTarget<RenderableToPOVRay> {
	
	private static final String INDENT = "  ";
	
	private final PrintStream output;
			
	public POVRayTarget(PrintStream output) {
		this.output = output;
	}
	
	@Override
	public Class<RenderableToPOVRay> getRenderableType() {
		return RenderableToPOVRay.class;
	}
	
	@Override
	public void render(RenderableToPOVRay renderable) {
		renderable.renderTo(this);
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
			String name = "texture_" + Materials.getUniqueName(material);

			append("#ifndef (" + name + ")\n");
			append("#declare " + name + " = ");
			appendMaterial(material);
			append("#end\n\n");
			
		}
		
	}
	
	@Override
	public void drawTriangles(Material material,
			Collection<? extends TriangleXYZ> triangles,
			List<List<VectorXZ>> textureCoordLists) {

		for (TriangleXYZ triangle : triangles) {
			performNaNCheck(triangle);
		}
		
		append("union {\n");

		for (TriangleXYZ triangle : triangles) {

			append(INDENT);
			
			appendTriangle(triangle.v1, triangle.v2, triangle.v3);

		}

		appendMaterialOrName(material);
		
		append("}\n");
		
	}

	@Override
	public void drawTrianglesWithNormals(Material material,
			Collection<? extends TriangleXYZWithNormals> triangles,
			List<List<VectorXZ>> textureCoordLists) {
		
		drawTrianglesWithNormals(material, triangles, textureCoordLists, false);
		
	}

	public void drawTrianglesWithNormals(Material material,
			Collection<? extends TriangleXYZWithNormals> triangles,
			List<List<VectorXZ>> textureCoordLists,
			boolean asMesh) {
		
		if (asMesh) {
			append("mesh {\n");
		} else {
			append("union {\n");
		}

		for (TriangleXYZWithNormals triangle : triangles) {

			append(INDENT);

			try {
				performNaNCheck(triangle);
				appendTriangle(triangle.v1, triangle.v2, triangle.v3,
						triangle.n1, triangle.n2, triangle.n3, true);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("caught in drawTrianglesWithNormals");
			}

		}

		appendMaterialOrName(material);
		
		append("}\n");

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
	public void drawPolygon(Material material, List<? extends VectorXYZ> vs,
			List<List<VectorXZ>> textureCoordLists) {
		
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
	
	public void appendTriangle(
			VectorXYZ a, VectorXYZ b, VectorXYZ c) {

		appendTriangle(a, b, c, null, null, null, false);

	}
	
	public void appendTriangle(
			VectorXYZ a, VectorXYZ b, VectorXYZ c,
			VectorXYZ na, VectorXYZ nb, VectorXYZ nc,
			boolean smooth) {

		// filter degenerate triangles
		
		if (a.equals(b) || a.equals(c) || b.equals(c)) {
			return;
		} else if (a.x == b.x && b.x == c.x
				&& a.y == b.y && b.y == c.y) {
			return;
		} else if (a.x == b.x && b.x == c.x
				&& a.z == b.z && b.z == c.z) {
			return;
		} else if (a.y == b.y && b.y == c.y
				&& a.z == b.z && b.z == c.z) {
			return;
		}
		
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
		
		append("  texture {\n");
		append("    pigment { ");
		appendRGBColor(material.getColor());
		append(" }\n    finish {\n");
		append("      ambient " + material.getAmbientFactor() + "\n");
		append("      diffuse " + material.getDiffuseFactor() + "\n");
		append("    }\n");
		append("  }\n");
		
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

	private void performNaNCheck(TriangleXYZWithNormals triangle) {
		performNaNCheck(triangle.v1);
		performNaNCheck(triangle.v2);
		performNaNCheck(triangle.v3);
		performNaNCheck(triangle.n1);
		performNaNCheck(triangle.n2);
		performNaNCheck(triangle.n3);
	}
	
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
