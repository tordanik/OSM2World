package org.osm2world.map_data.data.overlaps;

import java.util.Locale;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ID of an element from an OSM dataset.
 * The string representation consists of a numeric ID prefixed with a single letter:
 * n for node, w for way, r for relation.
 */
public record MapElementId(String string) {

	public static final Pattern TYPED_ID_PATTERN = Pattern.compile("^([nNwWrR])(-?\\d+)$");

	public enum ElementType {NODE, WAY, RELATION}

	public static @Nullable MapElementId parse(String id) {
		id = id.toLowerCase(Locale.ROOT);
		if (TYPED_ID_PATTERN.matcher(id).matches()) {
			return new MapElementId(id);
		} else {
			return null;
		}
	}

	public MapElementId {
		if (!TYPED_ID_PATTERN.matcher(string).matches()) {
			throw new IllegalArgumentException("Invalid ID: " + string);
		}
	}

	public ElementType type() {
		return switch (string.charAt(0)) {
			case 'n' -> ElementType.NODE;
			case 'w' -> ElementType.WAY;
			case 'r' -> ElementType.RELATION;
			default -> throw new IllegalStateException("Unknown element type prefix in id " + this);
		};
	}

	public long getId() {
		return Long.parseLong(string.substring(1));
	}

	@Override
	public @Nonnull String toString() {
		return string;
	}

}
