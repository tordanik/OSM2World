package org.osm2world.output;

import static java.util.Collections.nCopies;
import static java.util.Objects.requireNonNullElse;
import static org.osm2world.math.VectorXYZ.NULL_VECTOR;
import static org.osm2world.math.algorithms.GeometryUtil.trianglesFromTriangleFan;
import static org.osm2world.math.algorithms.GeometryUtil.trianglesFromTriangleStrip;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.GLOBAL_X_Y;
import static org.osm2world.scene.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.transformShape;

import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.GeometryUtil;
import org.osm2world.math.shapes.ClosedShapeXZ;
import org.osm2world.math.shapes.ShapeXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.math.shapes.TriangleXZ;
import org.osm2world.output.common.ExtrudeOption;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.MaterialOrRef;
import org.osm2world.scene.mesh.ExtrusionGeometry;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.mesh.MeshUtil;
import org.osm2world.scene.mesh.TriangleGeometry;
import org.osm2world.scene.model.ModelInstance;
import org.osm2world.world.data.ProceduralWorldObject;

/**
 * contains methods and functionality shared between {@link Output} classes and {@link ProceduralWorldObject.Target}.
 * This is a transitional solution: Classes representing output formats will move to a different solution.
 */
public interface CommonTarget {

	/**
	 * draws triangles.
	 *
	 * @param texCoordLists  one texture coordinate list per texture.
	 *          Each must have three coordinates per triangle.
	 */
	default void drawTriangles(@Nonnull MaterialOrRef material,
							   @Nonnull List<? extends TriangleXYZ> triangles,
							   @Nonnull List<List<VectorXZ>> texCoordLists) {
		drawMesh(new Mesh(new TriangleGeometry(new ArrayList<>(triangles), material.get().getInterpolation(),
				texCoordLists, null), material.get()));
	}

	default void drawTriangles(@Nonnull MaterialOrRef material,
							   @Nonnull List<? extends TriangleXYZ> triangles,
							   @Nonnull List<VectorXYZ> normals,
							   @Nonnull List<List<VectorXZ>> texCoordLists) {
		drawTriangles(material, triangles, texCoordLists);
	}

	/**
	 * draws a triangle strip.
	 *
	 * @param vs             vertices of the triangle strip
	 * @param texCoordLists  one texture coordinate list per texture.
	 *          Each must have the same length as the "vs" parameter.
	 */
	default void drawTriangleStrip(@Nonnull MaterialOrRef material, @Nonnull List<VectorXYZ> vs,
								   @Nonnull List<List<VectorXZ>> texCoordLists) {

		List<List<VectorXZ>> newTexCoordLists = texCoordLists.stream()
				.map(GeometryUtil::triangleVertexListFromTriangleStrip)
				.toList();

		drawTriangles(material, trianglesFromTriangleStrip(vs), newTexCoordLists);

	}

	/**
	 * draws a triangle fan.
	 *
	 * @see #drawTriangleStrip(MaterialOrRef, List, List)
	 */
	default void drawTriangleFan(@Nonnull MaterialOrRef material, @Nonnull List<VectorXYZ> vs,
						 @Nonnull List<List<VectorXZ>> texCoordLists) {

		List<List<VectorXZ>> newTexCoordLists = texCoordLists.stream()
				.map(GeometryUtil::triangleVertexListFromTriangleFan)
				.toList();

		drawTriangles(material, trianglesFromTriangleFan(vs), newTexCoordLists);

	}

	/**
	 * draws a <em>convex</em> polygon
	 *
	 * @see #drawTriangleStrip(MaterialOrRef, List, List)
	 */
	default void drawConvexPolygon(@Nonnull MaterialOrRef material, @Nonnull List<VectorXYZ> vs,
								   @Nonnull List<List<VectorXZ>> texCoordLists) {
		if (Objects.equals(vs.get(0), vs.get(vs.size() - 1))) {
			vs = vs.subList(0, vs.size() - 1);
			texCoordLists = texCoordLists.stream()
					.map(tcl -> tcl.subList(0, tcl.size() - 1))
					.toList();
		}
		drawTriangleFan(material, vs, texCoordLists);
	}

	/**
	 * draws a flat shape in 3D space, at an arbitrary rotation.
	 *
	 * @param material     the material used for the extruded shape; != null
	 * @param shape        the shape to be drawn; != null
	 * @param point        position where the shape is drawn; != null
	 * @param frontVector  direction the shape is facing.
	 *                     Defines the shape's rotation along with upVector; != null
	 * @param upVector     up direction of the shape.
	 *                     Defines the shape's rotation along with frontVector; != null
	 * @param scaleFactor  a factor to scale the shape by, 1.0 leaves the shape unscaled.
	 */
	default void drawShape(@Nonnull MaterialOrRef material, @Nonnull ClosedShapeXZ shape, @Nonnull VectorXYZ point,
				   @Nonnull VectorXYZ frontVector, @Nonnull VectorXYZ upVector, double scaleFactor) {

		for (TriangleXZ triangle : shape.getTriangulation()) {

			List<VectorXYZ> triangleVertices = new ArrayList<>();

			for (VectorXZ v : triangle.vertices()) {
				triangleVertices.add(new VectorXYZ(-v.x, v.z, 0));
			}

			if (scaleFactor != 1.0) {
				triangleVertices = scaleShapeVectors(triangleVertices, scaleFactor);
			}

			triangleVertices = transformShape(
					triangleVertices, point, frontVector, upVector);

			//TODO better default texture coordinate function
			drawTriangleStrip(material, triangleVertices.subList(0, 3),
					texCoordLists(triangleVertices.subList(0, 3), material.get(), GLOBAL_X_Y));

		}

	}

	/**
	 * extrudes a 2d shape along a path.
	 *
	 * <p>For problematic input parameters, the resulting geometry might end up
	 * self-intersecting or contain zero-area triangles.
	 *
	 * @param  material       the material used for the extruded shape; != null
	 * @param  shape          the shape to be extruded; != null
	 * @param  path           the path along which the shape is extruded. Implicitly,
	 *                        this also defines a rotation for the shape at each point.
	 *                        Must have at least two points; != null.
	 * @param  upVectors      defines the rotation (along with the path) at each point.
	 *                        Must have the same number of elements as path.
	 *                        You can use {@link Collections#nCopies(int, Object)}.
	 *                        Can be null if the path is vertical (defaults to z unit vector).
	 *                        if you want the same up vector for all points of the path.
	 * @param  scaleFactors   optionally allows the shape to be scaled at each point.
	 *                        Must have the same number of elements as path.
	 *                        Can be set to null for a constant scale factor of 1
	 * @param  options        flags setting additional options; can be null for no options.
	 *                        Usually an {@link EnumSet}.
	 *
	 * @throws IllegalArgumentException  if upVectors are null and cannot be inferred
	 *                                   from the path. This happens for completely vertical
	 *                                   or otherwise ambiguous paths.
	 */
	default void drawExtrudedShape(@Nonnull MaterialOrRef material, @Nonnull ShapeXZ shape, @Nonnull List<VectorXYZ> path,
			@Nullable List<VectorXYZ> upVectors, @Nullable List<Double> scaleFactors,
			@Nullable Set<ExtrudeOption> options) {

		if (material.get().getInterpolation() == Material.Interpolation.SMOOTH) {
			options = requireNonNullElse(options, EnumSet.noneOf(ExtrudeOption.class));
			options.add(ExtrudeOption.SMOOTH_SIDES);
		}

		drawMesh(new Mesh(new ExtrusionGeometry(shape, path, upVectors, scaleFactors, null, options,
				material.get().getTextureDimensions()), material.get()));

	}

	/**
	 * draws a box with outward-facing polygons.
	 *
	 * @param faceDirection  direction for the "front" of the box
	 */
	default void drawBox(@Nonnull MaterialOrRef material, @Nonnull VectorXYZ bottomCenter, @Nonnull VectorXZ faceDirection,
						 double height, double width, double depth) {

		drawMesh(new Mesh(MeshUtil.createBox(bottomCenter, faceDirection, height, width, depth, null,
				material.get().getTextureDimensions()), material.get()));

	}

	/**
	 * draws a column with outward-facing polygons around a point.
	 * A column is a polygon with 3 or more corners extruded upwards.
	 *
	 * The implementation may decide to reduce the number of corners
	 * in order to improve performance (or make rendering possible
	 * when a perfect cylinder isn't supported).
	 *
	 * @param corners  number of corners; null creates a cylinder
	 *  for radiusBottom == radiusTop or (truncated) cone otherwise
	 */
	default void drawColumn(@Nonnull MaterialOrRef material, @Nullable Integer corners,
							@Nonnull VectorXYZ base, double height, double radiusBottom,
							double radiusTop, boolean drawBottom, boolean drawTop) {

		drawMesh(new Mesh(ExtrusionGeometry.createColumn(corners, base, height, radiusBottom, radiusTop, drawBottom, drawTop,
				null, material.get().getTextureDimensions()), material.get()));

	}

	/**
	 * draws an instanced model.
	 */
	default void drawModel(ModelInstance modelInstance) {
		modelInstance.render(this);
	}

	void drawMesh(Mesh mesh);

	static List<VectorXYZ> scaleShapeVectors(List<VectorXYZ> vs, double scale) {

		if (scale == 1) {

			return vs;

		} else if (scale == 0) {

			return nCopies(vs.size(), NULL_VECTOR);

		} else {

			List<VectorXYZ> result = new ArrayList<>(vs.size());

			for (VectorXYZ v : vs) {
				result.add(v.mult(scale));
			}

			return result;

		}

	}

}
