package org.osm2world.core.target.common;

import java.util.List;
import java.util.Set;

import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;

/**
 * Flags describing available options for
 * {@link Target#drawExtrudedShape(Material, ShapeXZ, List, List, List, List, Set)}.
 */
public enum ExtrudeOption {

	/**
	 * determines whether the beginning of the "pipe" created by the extrusion should be capped
	 * or left open. Only works for simple, closed shapes (e.g. polygons or circles).
	 */
	START_CAP,

	/**
	 * determines whether the end of the "pipe" created by the extrusion should be capped
	 * or left open. Only works for simple, closed shapes (e.g. polygons or circles).
	 */
	END_CAP

}
