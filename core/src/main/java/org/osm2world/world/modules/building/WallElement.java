package org.osm2world.world.modules.building;

import static org.osm2world.world.data.ProceduralWorldObject.Target;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.math.shapes.SimplePolygonXZ;

/**
 * something that can be placed into a wall, such as a window or door
 */
interface WallElement {

	/**
	 * returns the space on the 2D {@link WallSurface} occupied by this element.
	 * The element is responsible for handling rendering inside this area.
	 */
	public SimplePolygonXZ outline();

	/**
	 * how deep the element is sunk into the wall. Will be used to render the bits of wall around it.
	 * Can be 0 if the element is flat on the wall, or handles its own rendering of the inset walls.
	 */
	public double insetDistance();

	public void renderTo(Target target, WallSurface surface, O2WConfig config);

}