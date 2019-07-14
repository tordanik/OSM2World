package org.osm2world.core.map_elevation.creation;

import static org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser.parseOsmDecimal;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;

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
