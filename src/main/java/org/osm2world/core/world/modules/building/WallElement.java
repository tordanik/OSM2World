package org.osm2world.core.world.modules.building;

import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.target.Target;

/**
 * something that can be placed into a wall, such as a window or door
 */
interface WallElement {

	/**
	 * returns the space on the 2D {@link WallSurface} occupied by this element.
	 * The element is responsible for handling rendering inside this area.
	 */
	public SimplePolygonXZ outline();

	public void renderTo(Target target, WallSurface surface);

}