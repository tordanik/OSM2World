package org.osm2world.core.map_elevation.creation;

import static org.osm2world.core.util.ValueParseUtil.parseOsmDecimal;

import org.osm2world.core.map_data.data.TagSet;

/**
 * sets elevations based on level tags
 */
public class LevelTagElevationCalculator extends TagElevationCalculator {

	final double elePerLevel;

	public LevelTagElevationCalculator(double elePerLevel) {
		super(0.0, true);
		this.elePerLevel = elePerLevel;
	}

	public LevelTagElevationCalculator() {
		this(3.0);
	}

	@Override
	protected Double getEleForTags(TagSet tags) {

		Float value = null;

		if (tags.containsKey("level")) {
			value = parseOsmDecimal(tags.getValue("level"), true);
		}

		if (value == null) {
			return null;
		} else {
			return elePerLevel * value;
		}

	}

}
