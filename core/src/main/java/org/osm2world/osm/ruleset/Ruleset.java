package org.osm2world.osm.ruleset;

import de.topobyte.osm4j.core.model.iface.OsmTag;
import org.osm2world.map_data.data.Tag;
import org.osm2world.map_data.data.TagSet;

public interface Ruleset {

	/** identifies tags that indicate that a closed way represents an area */
	public boolean isAreaTag(Tag tag);

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
	public boolean isRelevantRelation(TagSet tags);

}
