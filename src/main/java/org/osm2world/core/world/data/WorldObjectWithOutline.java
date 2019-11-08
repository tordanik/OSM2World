package org.osm2world.core.world.data;

import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.shapes.PolygonShapeXZ;

/**
 * world object that has a defined outline that can be used for purposes
 * such as cutting holes into the terrain (if this is instance of
 * {@link TerrainBoundaryWorldObject}), cutting tunnels through buildings,
 * preventing bridge pillars from piercing through this WorldObject ...
 */
public interface WorldObjectWithOutline extends WorldObject {

	/**
	 * returns a counterclockwise polygon defining the object's ground footprint.
	 *
	 * @return outline polygon; null if this world object doesn't cover any area
	 */
	public PolygonXYZ getOutlinePolygon();

	/**
	 * returns a counterclockwise polygon defining the object's ground footprint,
	 * projected onto the XZ plane.
	 *
	 * @return outline polygon; null if this world object doesn't cover any area
	 */
	public PolygonShapeXZ getOutlinePolygonXZ();

}
