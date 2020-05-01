package org.osm2world.core.world.modules.building;

/**
 * how windows on building walls should be implemented.
 * This represents a trade-off between visuals and various performance factors.
 */
enum WindowImplementation {

	/** no windows at all */
	NONE,
	/** a repeating texture image on a flat wall */
	FLAT_TEXTURES,
	/** windows using a combination of geometry and textures */
	INSET_TEXTURES,
	/** windows with actual geometry */
	FULL_GEOMETRY;

	public static WindowImplementation getValue(String value, WindowImplementation defaultValue) {

		if (value != null) {
			try {
				return WindowImplementation.valueOf(value.toUpperCase());
			} catch (IllegalArgumentException e) {}
		}

		return defaultValue;

	}

}