package org.osm2world.core.target;

import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.VectorXYZ;
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
	
	void drawTriangles(Material material, Collection<? extends TriangleXYZ> triangles);

	void drawTrianglesWithNormals(Material material, Collection<? extends TriangleXYZWithNormals> triangles);

	void drawTriangleStrip(Material material, VectorXYZ... vs); //TODO: delete this, use only collections
	
	void drawTriangleStrip(Material material, List<? extends VectorXYZ> vs);
	
	void drawTriangleFan(Material material, List<? extends VectorXYZ> vs);
	
	void drawPolygon(Material material, VectorXYZ... vs);

	/**
	 * draws a box with outward-facing polygons
	 */
	void drawBox(Material material, VectorXYZ frontLowerLeft,
			VectorXYZ rightVector, VectorXYZ upVector, VectorXYZ backVector);

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
	
}
