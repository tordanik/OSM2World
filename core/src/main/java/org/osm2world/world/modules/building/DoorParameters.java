package org.osm2world.world.modules.building;

import static java.util.Arrays.asList;
import static org.osm2world.scene.color.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.util.ValueParseUtil.parseColor;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.parseHeight;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.parseWidth;

import javax.annotation.Nullable;

import org.apache.commons.lang3.math.NumberUtils;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.scene.color.Color;

/**
 * data about a door.
 * This object type is immutable after its construction from a set of tags.
 */
public class DoorParameters {

	public final String type;
	public final @Nullable String materialName;
	public final @Nullable Color color;

	public final double width;
	public final double height;
	public final double inset;

	public final int numberOfWings;

	private DoorParameters(String type, String materialName, Color color,
			double width, double height, double inset, int numberOfWings) {
		this.type = type;
		this.materialName = materialName;
		this.color = color;
		this.width = width;
		this.height = height;
		this.inset = inset;
		this.numberOfWings = numberOfWings;
	}

	/**
	 * extracts door parameters from a set of tags
	 * and (optionally) another set of tags describing the building part and/or wall the door is in
	 */
	public static DoorParameters fromTags(TagSet tags, @Nullable TagSet parentTags) {

		/* determine the type */

		String type = "hinged";

		if (parentTags != null) {
			if (parentTags.containsAny(
					asList("building", "building:part"),
					asList("garage", "garages"))) {
				type = "overhead";
			}
		}

		if (tags.containsKey("door") && !"yes".equals(tags.getValue("door"))) {
			type = tags.getValue("door");
		}

		/* determine material and other attributes */

		String materialName = null;
		if (tags.containsKey("material")) {
			materialName = tags.getValue("material");
		}

		Color color = parseColor(tags.getValue("colour"), CSS_COLORS);

		int numberOfWings = NumberUtils.toInt(tags.getValue("door:wings"), 1);

		/* parse or estimate width and height */

		double defaultWidth = 1.0;
		double defaultHeight = 2.0;
		double defaultInset = 0.1;

		switch (type) {
		case "overhead":
			defaultWidth = 2.5;
			defaultHeight = 2.125;
			break;
		case "hinged":
			if (numberOfWings == 2) {
				defaultWidth *= 2;
			}
			break;
		}

		double width = parseWidth(tags, (float)defaultWidth);
		double height = parseHeight(tags, (float)defaultHeight);

		/* return the result */

		return new DoorParameters(type, materialName, color, width, height, defaultInset, numberOfWings);

	}

	public DoorParameters withInset(double inset) {
		return new DoorParameters(type, materialName, color, width, height, inset, numberOfWings);
	}

}
