package org.osm2world.core.target;

import java.util.Collection;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.data.WorldObject;

/**
 * @param <R>  subtype of {@link Renderable} designed for visualization
 *             with this target
 */
public interface Target<R extends Renderable> {
	
	/**
	 * returns the renderable type designed for this target
	 */
	Class<R> getRenderableType();

	void setConfiguration(Configuration config);
	
	/**
	 * renders a renderable object to this target.
	 * Usually, this means calling a "renderTo" method on that renderable,
	 * with this target as a parameter.
	 */
	void render(R renderable);
	
	/**
	 * announces the begin of the draw* calls for a {@link WorldObject}.
	 * This allows targets to group them, if desired.
	 * Otherwise, this can be ignored.
	 */
	void beginObject(WorldObject object);
	
	/**
	 * draws triangles.
	 * 
	 * @param texCoordLists  one texture coordinate list per texture.
	 *          Each must have three coordinates per triangle.
	 *          Can be null if no texturing information is available.
	 */
	void drawTriangles(Material material,
			Collection<? extends TriangleXYZ> triangles,
			List<List<VectorXZ>> texCoordLists);
	
	/**
	 * draws triangles with explicitly defined normal vectors.
	 * 
	 * @see #drawTriangles(Material, Collection, List)
	 */
	void drawTrianglesWithNormals(Material material,
			Collection<? extends TriangleXYZWithNormals> triangles,
			List<List<VectorXZ>> texCoordLists);
	
	/**
	 * draws a triangle strip.
	 * 
	 * @param vs             vertices of the triangle strip
	 * @param texCoordsList  one texture coordinate list per texture.
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
