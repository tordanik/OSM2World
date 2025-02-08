package org.osm2world.util.color;

import java.awt.Color;

/** a mapping from color names to RGB colors */
public interface ColorNameDefinition {

	/** returns true if the given name is a known color name */
	public default boolean contains(String name) {
		return get(name) != null;
	}

	/** returns the RGB color for that name, or null if it isn't a known color name */
	public Color get(String name);

}
