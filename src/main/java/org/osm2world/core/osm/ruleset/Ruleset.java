package org.osm2world.core.osm.ruleset;

import org.openstreetmap.josm.plugins.graphview.core.data.Tag;

public interface Ruleset {

	/** identifies tags that indicate that a closed way represents an area */
	public boolean isAreaTag(Tag tag);

	/**
	 * identifies tags which (almost) exclusively appear outside the sea.
	 * This lets us make a guess whether a tile is a land tile.
	 */
	public boolean isLandTag(Tag tag);

	/**
	 * identifies tags which (almost) exclusively appear on the sea.
	 * This lets us make a guess whether a tile is a land tile.
	 */
	public boolean isSeaTag(Tag tag);

}
