package org.osm2world.core.target;

import static java.util.Collections.nCopies;
import static org.osm2world.core.math.GeometryUtil.trianglesFromTriangleFan;
import static org.osm2world.core.math.GeometryUtil.trianglesFromTriangleStrip;
import static org.osm2world.core.math.VectorXYZ.NULL_VECTOR;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Y;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.transformShape;

import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.core.math.*;
import org.osm2world.core.math.shapes.ClosedShapeXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.common.ExtrudeOption;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.mesh.ExtrusionGeometry;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.mesh.MeshUtil;
import org.osm2world.core.target.common.mesh.TriangleGeometry;
import org.osm2world.core.target.common.model.InstanceParameters;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.world.data.ProceduralWorldObject;

/**
 * contains methods and functionality shared between {@link Target} and {@link ProceduralWorldObject.Target}.
 * This is a transitional solution: Classes representing output formats will move to a different solution.
 */
public interface CommonTarget {

	/**
	 * draws triangles.
	 *
	 * @param texCoordLists  one texture coordinate list per texture.
	 *          Each must have three coordinates per triangle.
	 */
	default void drawTriangles(@Nonnull Material material,
							   @Nonnull List<? extends TriangleXYZ> triangles,
							   @Nonnull List<List<VectorXZ>> texCoordLists) {
		drawMesh(new Mesh(new TriangleGeometry(new ArrayList<>(triangles), material.getInterpolation(),
				texCoordLists, null), material));
	}

	default void drawTriangles(@Nonnull Material material,
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
	default void drawTriangleStrip(@Nonnull Material material, @Nonnull List<VectorXYZ> vs,
								   @Nonnull List<List<VectorXZ>> texCoordLists) {

		List<List<VectorXZ>> newTexCoordLists = texCoordLists.stream()
				.map(GeometryUtil::triangleVertexListFromTriangleStrip)
				.toList();

		drawTriangles(material, trianglesFromTriangleStrip(vs), newTexCoordLists);

	}

	/**
	 * draws a triangle fan.
	 *
	 * @see #drawTriangleStrip(Material, List, List)
	 */
	default void drawTriangleFan(@Nonnull Material material, @Nonnull List<VectorXYZ> vs,
						 @Nonnull List<List<VectorXZ>> texCoordLists) {

		List<List<VectorXZ>> newTexCoordLists = texCoordLists.stream()
				.map(GeometryUtil::triangleVertexListFromTriangleFan)
				.toList();

		drawTriangles(material, trianglesFromTriangleFan(vs), newTexCoordLists);

	}

	/**
	 * draws a <em>convex</em> polygon
	 *
	 * @see #drawTriangleStrip(Material, List, List)
	 */
	default void drawConvexPolygon(@Nonnull Material material, @Nonnull List<VectorXYZ> vs,
								   @Nonnull List<List<VectorXZ>> texCoordLists) {
		if (Objects.equals(vs.get(0), vs.get(vs.size() - 1))) {
			vs = vs.subList(0, vs.size() - 1);
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
	default void drawShape(@Nonnull Material material, @Nonnull ClosedShapeXZ shape, @Nonnull VectorXYZ point,
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
					texCoordLists(triangleVertices.subList(0, 3), material, GLOBAL_X_Y));

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
	 * @param  texCoordLists  one texture coordinate list per texture.
	 *                        The number of vectors in each must be equal to the number of
	 *                        vertices of the shape, multiplied by the length of the path.
	 *                        Can be null, in which case it falls back to a default.
	 * @param  options        flags setting additional options; can be null for no options.
	 *                        Usually an {@link EnumSet}.
	 *
	 * @throws IllegalArgumentException  if upVectors are null and cannot be inferred
	 *                                   from the path. This happens for completely vertical
	 *                                   or otherwise ambiguous paths.
	 */
	default void drawExtrudedShape(@Nonnull Material material, @Nonnull ShapeXZ shape, @Nonnull List<VectorXYZ> path,
								   @Nullable List<VectorXYZ> upVectors, @Nullable List<Double> scaleFactors,
								   @Nullable List<List<VectorXZ>> texCoordLists, @Nullable Set<ExtrudeOption> options) {

		drawMesh(new Mesh(new ExtrusionGeometry(shape, path, upVectors, scaleFactors, null, options,
				material.getTextureDimensions()), material));

	}

	/**
	 * draws a box with outward-facing polygons.
	 *
	 * @param faceDirection  direction for the "front" of the box
	 */
	default void drawBox(@Nonnull Material material, @Nonnull VectorXYZ bottomCenter, @Nonnull VectorXZ faceDirection,
						 double height, double width, double depth) {

		drawMesh(new Mesh(MeshUtil.createBox(bottomCenter, faceDirection, height, width, depth, null,
				material.getTextureDimensions()), material));

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
	default void drawColumn(@Nonnull Material material, @Nullable Integer corners,
							@Nonnull VectorXYZ base, double height, double radiusBottom,
							double radiusTop, boolean drawBottom, boolean drawTop) {

		drawMesh(new Mesh(ExtrusionGeometry.createColumn(corners, base, height, radiusBottom, radiusTop, drawBottom, drawTop,
				null, material.getTextureDimensions()), material));

	}

	/**
	 * draws an instanced model.
	 */
	default void drawModel(Model model, InstanceParameters params) {
		model.render(this, params);
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
