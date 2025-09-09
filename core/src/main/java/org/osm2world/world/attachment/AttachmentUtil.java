package org.osm2world.world.attachment;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapElement;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.util.ValueParseUtil;

/**
 * methods for working with {@link AttachmentConnector}s
 */
public final class AttachmentUtil {

	/** prevents instantiation */
	private AttachmentUtil() {}

	/**
	 * builds the list of possible attachment types for a {@link MapElement} based on support, location and other tags.
	 */
	public static List<String> getCompatibleSurfaceTypes(MapElement element) {

		List<String> types = new ArrayList<>();
		TagSet tags = element.getTags();

		List<Integer> levels = ValueParseUtil.parseLevels(tags.getValue("level"));
		Integer level = levels != null ? levels.get(0) : null;

		/* handle support */

		String support = tags.getValue("support");
		if ("wall".equals(support)) {
			if (level != null) {
				types.add("wall" + level);
			}
			types.add("wall");
		} else if (support != null && !"ground".equals(support)) {
			types.add(support);
		}

		/* handle rooftop features */

		if (tags.contains("location", "roof") || tags.contains("location", "rooftop") || tags.contains("parking", "rooftop")) {
			if (level != null) {
				types.add("roof" + level);
			}
			types.add("roof");
		}

		/* handle generic indoor indoor features */

		if (types.isEmpty() && level != null) {
			types.add("floor" + level);
		}

		return types;

	}

	/**
	 * returns true if the list (also) contains attachment surface types which are typically vertical.
	 * This allows different behavior (e.g. different preferred height).
	 */
	public static boolean hasVerticalSurfaceTypes(List<String> attachmentSurfaceTypes) {
		return attachmentSurfaceTypes.stream().anyMatch(t -> !t.startsWith("roof") && !t.startsWith("floor"));
	}

	/**
	 * Returns true if the attachment connector is attached to a vertical surface
	 */
	public static boolean isAttachedToVerticalSurface(@Nullable AttachmentConnector connector) {
		return connector != null && connector.isAttached()
				&& connector.getAttachedSurfaceNormal().y < 0.8;
	}

}
