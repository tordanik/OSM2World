package org.osm2world.core.world.modules.building;

import static org.osm2world.core.util.ValueParseUtil.*;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;

/**
 * data about the window(s) on a wall, door, or for a single window.
 * This object type is immutable after its construction from a set of tags.
 */
public class WindowParameters {

	private static enum WindowType {
		PLAIN, DISPLAY_WINDOW
	}

	static enum WindowShape {

		RECTANGULAR, ROUND;

		/**
		 * convenient case-insensitive and exception-free alternative to valueOf
		 * @return  the matching value, can be null
		 */
		public static WindowParameters.WindowShape getValue(String shapeName) {

			if (shapeName == null) {
				return null;
			}

			try {
				return valueOf(shapeName.toUpperCase());
			} catch (IllegalArgumentException e) {
				return null;
			}

		}

	}

	private static final double DEFAULT_WIDTH = 1;
	private static final double DEFAULT_HEIGHT_RELATIVE_TO_LEVEL = 0.5f;
	private static final double DEFAULT_BREAST_RELATIVE_TO_LEVEL = 0.3f;

	public final WindowParameters.WindowType type;

	public final boolean hasLeftShutter;
	public final boolean hasRightShutter;

	public final @Nullable Integer numberWindows;
	public final int groupSize;

	public final double width;
	public final double height;
	public final double breast;

	public final int panesHorizontal;
	public final int panesVertical;

	public final WindowParameters.WindowShape windowShape;

	/** the material to use for the window pane if it should appear opaque (non-transparent) */
	public final Material opaqueWindowMaterial;
	/** the material to use for the window pane if it should appear transparent */
	public final Material transparentWindowMaterial;

	public final Material frameMaterial;
	public final Material shutterMaterial;

	public WindowParameters(TagSet tags, double levelHeight) {

		/* window */

		String windowString = tags.getValue("window");

		if ("display_window".equals(windowString)) {
			type = WindowType.DISPLAY_WINDOW;
		} else {
			type = WindowType.PLAIN;
		}

		opaqueWindowMaterial = BuildingPart.buildMaterial(
				tags.getValue("window:material"),
				tags.getValue("window:colour"),
				Materials.GLASS, false);

		transparentWindowMaterial = BuildingPart.buildMaterial(
				null,
				tags.getValue("window:colour"),
				Materials.GLASS_TRANSPARENT, false);

		numberWindows = parseUInt(tags.getValue("window:count"));
		groupSize = parseUInt(tags.getValue("window:group_size"), 1);

		width = parseMeasure(tags.getValue("window:width"), DEFAULT_WIDTH);
		height = parseMeasure(tags.getValue("window:height"), DEFAULT_HEIGHT_RELATIVE_TO_LEVEL * levelHeight);
		breast = parseMeasure(tags.getValue("window:breast"), DEFAULT_BREAST_RELATIVE_TO_LEVEL * levelHeight);


		String panesValue = tags.getValue("window:panes");

		if (panesValue != null && panesValue.matches("^\\d+x\\d+$")) {
			String[] s = panesValue.split("x");
			panesHorizontal = parseUInt(s[0], 1);
			panesVertical = parseUInt(s[1], 1);
		} else {
			panesVertical = 1;
			panesHorizontal = 2;
		}

		WindowParameters.WindowShape tempWindowShape = WindowShape.getValue(tags.getValue("window:shape"));
		windowShape = tempWindowShape != null ? tempWindowShape : WindowShape.RECTANGULAR;

		/* frame */

		frameMaterial = BuildingPart.buildMaterial(
				tags.getValue("window:frame:material"),
				tags.getValue("window:frame:colour"),
				Materials.PLASTIC, false);

		/* shutters */

		if (tags.contains("window:shutter", "both")) {
			hasLeftShutter = true;
			hasRightShutter = true;
		} else if (tags.contains("window:shutter", "left")) {
			hasLeftShutter = true;
			hasRightShutter = false;
		} else if (tags.contains("window:shutter", "right")) {
			hasLeftShutter = false;
			hasRightShutter = true;
		} else {
			hasLeftShutter = false;
			hasRightShutter = false;
		}

		shutterMaterial = BuildingPart.buildMaterial(
				tags.getValue("window:shutter:material"),
				tags.getValue("window:shutter:colour"),
				Materials.WOOD, false);

	}

}
