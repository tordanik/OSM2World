package org.osm2world.core.osm.ruleset;

import de.topobyte.osm4j.core.model.iface.OsmTag;

public interface Ruleset {

	/** identifies tags that indicate that a closed way represents an area */
	public boolean isAreaTag(OsmTag tag);

	/**
	 * identifies tags which (almost) exclusively appear outside the sea.
	 * This lets us make a guess whether a tile is a land tile.
	 */
	public boolean isLandTag(OsmTag tag);

	/**
	 * identifies tags which (almost) exclusively appear on the sea.
	 * This lets us make a guess whether a tile is a land tile.
	 */
	public boolean isSeaTag(OsmTag tag);

	/** checks if this relation type is considered relevant for OSM2World */
	public boolean isWhitelistedRelationType(String type);

}
