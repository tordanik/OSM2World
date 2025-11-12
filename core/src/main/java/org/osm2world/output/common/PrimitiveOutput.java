package org.osm2world.output.common;

import static org.osm2world.math.algorithms.NormalCalculationUtil.*;
import static org.osm2world.output.common.Primitive.Type.*;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.scene.material.Material.Interpolation;
import org.osm2world.scene.material.MaterialOrRef;

/**
 * superclass for output formats that are based on OpenGL primitives.
 * These outputs will treat different primitives similarly:
 * They convert them all to a list of vertices
 * and represent the primitive type using an enum or flags.
 */
public abstract class PrimitiveOutput extends AbstractOutput implements DrawBasedOutput {

	/**
	 * @param vs       vertices that form the primitive
	 * @param normals  normal vector for each vertex; same size as vs
	 * @param texCoordLists  texture coordinates for each texture layer,
	 *                       each list has the same size as vs
	 */
	abstract protected void drawPrimitive(Primitive.Type type, MaterialOrRef material,
			List<VectorXYZ> vs, List<VectorXYZ> normals,
			List<List<VectorXZ>> texCoordLists);

	@Override
	public void drawTriangleStrip(@Nonnull MaterialOrRef material, @Nonnull List<VectorXYZ> vs,
								  @Nonnull List<List<VectorXZ>> texCoordLists) {
		boolean smooth = (material.get().interpolation() == Interpolation.SMOOTH);
		drawPrimitive(TRIANGLE_STRIP, material, vs,
				calculateTriangleStripNormals(vs, smooth),
				texCoordLists);
	}

	@Override
	public void drawTriangleFan(@Nonnull MaterialOrRef material, @Nonnull List<VectorXYZ> vs,
								@Nonnull List<List<VectorXZ>> texCoordLists) {
		boolean smooth = (material.get().interpolation() == Interpolation.SMOOTH);
		drawPrimitive(TRIANGLE_FAN, material, vs,
				calculateTriangleFanNormals(vs, smooth),
				texCoordLists);
	}

	@Override
	public void drawTriangles(@Nonnull MaterialOrRef material,
							  @Nonnull List<? extends TriangleXYZ> triangles,
							  @Nonnull List<List<VectorXZ>> texCoordLists) {
		drawTriangles(material, triangles,
				calculateTriangleNormals(triangles, material.get().interpolation() == Interpolation.SMOOTH),
				texCoordLists);
	}

	@Override
	public void drawTriangles(@Nonnull MaterialOrRef material,
							  @Nonnull List<? extends TriangleXYZ> triangles,
							  @Nonnull List<VectorXYZ> normals,
							  @Nonnull List<List<VectorXZ>> texCoordLists) {

		List<VectorXYZ> vectors = new ArrayList<>(triangles.size() * 3);

		for (TriangleXYZ triangle : triangles) {
			vectors.add(triangle.v1);
			vectors.add(triangle.v2);
			vectors.add(triangle.v3);
		}

		drawPrimitive(TRIANGLES, material, vectors, normals, texCoordLists);

	}

}
