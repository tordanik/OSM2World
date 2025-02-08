package org.osm2world.map_elevation.creation;

import static org.osm2world.util.ValueParseUtil.parseOsmDecimal;

import org.osm2world.map_data.data.TagSet;

/**
 * sets elevations based on ele tags
 */
public class EleTagEleCalculator extends TagEleCalculator {

	@Override
	protected Double getEleForTags(TagSet tags, double terrainEle) {
		if (tags.containsKey("ele")) {
			return parseOsmDecimal(tags.getValue("ele"), null);
		} else {
			return null;
		}
	}

}
