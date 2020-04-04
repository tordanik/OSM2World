package org.osm2world.core.map_elevation.creation;

import static org.osm2world.core.util.ValueParseUtil.parseOsmDecimal;

import org.osm2world.core.map_data.data.TagGroup;

/**
 * sets elevations based on ele tags
 */
public class EleTagElevationCalculator extends TagElevationCalculator {

	@Override
	protected Double getEleForTags(TagGroup tags) {

		Float value = null;

		if (tags.containsKey("ele")) {
			value = parseOsmDecimal(tags.getValue("ele"), true);
		}

		if (value == null) {
			return null;
		} else {
			return (double) value;
		}

	}

}
