package org.osm2world.util.enums;

import javax.annotation.Nullable;

public enum ForwardBackward {

	FORWARD, BACKWARD;

	public ForwardBackward invert() {
		return this == FORWARD ? BACKWARD : FORWARD;
	}

	public static @Nullable ForwardBackward of(String s) {
		if (s == null) return null;
		try {
			return ForwardBackward.valueOf(s.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

}
