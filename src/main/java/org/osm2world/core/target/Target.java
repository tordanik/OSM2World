package org.osm2world.core.target;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.ClosedShapeXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.common.ExtrudeOption;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.data.WorldObject;

/**
 * A sink for rendering/writing {@link WorldObject}s to.
 */
public interface Target {

	void setConfiguration(Configuration config);

	/**
	 * announces the begin of the draw* calls for a {@link WorldObject}.
	 * This allows targets to group them, if desired.
	 * Otherwise, this can be ignored.
	 *
	 * @param object  the object that all draw method calls until the next beginObject belong to; can be null
	 */
	default void beginObject(WorldObject object) {}

	/**
	 * draws triangles.
	 *
	 * @param texCoordLists  one texture coordinate list per texture.
	 *          Each must have three coordinates per triangle.
	 *          Can be null if no texturing information is available.
	 */
	void drawTriangles(Material material,
			List<? extends TriangleXYZ> triangles,
			List<List<VectorXZ>> texCoordLists);

	/**
	 * draws a triangle strip.
	 *
	 * @param vs             vertices of the triangle strip
	 * @param texCoordLists  one texture coordinate list per texture.
	 *          Each must have the same length as the "vs" parameter.
	 *          Can be null if no texturing information is available.
	 */
	void drawTriangleStrip(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists);

	/**
	 * draws a triangle fan.
	 *
	 * @see #drawTriangleStrip(Material, List, List)
	 */
	void drawTriangleFan(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists);

	/**
	 * draws a <em>convex</em> polygon
	 *
	 * @see #drawTriangleStrip(Material, List, List)
	 */
	void drawConvexPolygon(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists);

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
	void drawShape(Material material, ClosedShapeXZ shape, VectorXYZ point,
			VectorXYZ frontVector, VectorXYZ upVector, double scaleFactor);

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
	void drawExtrudedShape(Material material, ShapeXZ shape, List<VectorXYZ> path,
			List<VectorXYZ> upVectors, List<Double> scaleFactors,
			List<List<VectorXZ>> texCoordLists, Set<ExtrudeOption> options);

	/**
	 * draws a box with outward-facing polygons.
	 *
	 * @param faceDirection  direction for the "front" of the box
	 */
	void drawBox(Material material,
			VectorXYZ bottomCenter, VectorXZ faceDirection,
			double height, double width, double depth);

	/**
	 * draws a column with outward-facing polygons around a point.
	 * A column is a polygon with > 3 corners extruded upwards.
	 *
	 * The implementation may decide to reduce the number of corners
	 * in order to improve performance (or make rendering possible
	 * when a perfect cylinder isn't supported).
	 * @param corners  number of corners; null creates a cylinder
	 *  for radiusBottom == radiusTop or (truncated) cone otherwise
	 */
	void drawColumn(Material material, Integer corners,
			VectorXYZ base, double height, double radiusBottom,
			double radiusTop, boolean drawBottom, boolean drawTop);

	/**
	 * gives the target the chance to perform finish/cleanup operations
	 * after all objects have been drawn.
	 */
	void finish();

}
