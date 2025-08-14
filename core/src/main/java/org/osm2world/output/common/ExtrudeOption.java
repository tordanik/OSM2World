package org.osm2world.output.common;

/**
 * Flags describing available options for
 * {@link org.osm2world.scene.mesh.ExtrusionGeometry}.
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
	END_CAP,

	/**
	 * whether the sides of the extruded geometry should have smooth normals
	 */
	SMOOTH_SIDES,

	/**
	 * whether the caps (if any) should have smooth normals at the vertices they share with the sides
	 */
	SMOOTH_CAPS,

	/**
	 * whether the texture's height dimension should be along the path (instead of along the shape's ring)
	 */
	TEX_HEIGHT_ALONG_PATH

}
