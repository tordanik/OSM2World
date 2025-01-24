package org.osm2world.core.util.enums;

import javax.annotation.Nullable;

public enum LeftRight {

	LEFT, RIGHT;

	public LeftRight invert() {
		return this == LEFT ? RIGHT : LEFT;
	}

	public static @Nullable LeftRight of(String s) {
		if (s == null) return null;
		try {
			return LeftRight.valueOf(s.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

}
