package org.osm2world.core.util.enums;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

public enum LeftRightBoth {

	LEFT, RIGHT, BOTH;

	public static @Nullable LeftRightBoth of(String s) {
		if (s == null) return null;
		try {
			return LeftRightBoth.valueOf(s.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	public static LeftRightBoth of(LeftRight value) {
		switch (requireNonNull(value)) {
		case LEFT: return LEFT;
		case RIGHT: return RIGHT;
		default: throw new Error("unknown value");
		}
	}

	public static @Nullable LeftRightBoth ofNullable(@Nullable LeftRight value) {
		if (value == null) return null;
		switch (value) {
		case LEFT: return LEFT;
		case RIGHT: return RIGHT;
		default: throw new Error("unknown value");
		}
	}

	public boolean isLeftOrBoth() {
		return this == LEFT || this == BOTH;
	}

	public boolean isRightOrBoth() {
		return this == RIGHT || this == BOTH;
	}

}
