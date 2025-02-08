package org.osm2world.util.enums;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

public enum ForwardBackwardBoth {

	FORWARD, BACKWARD, BOTH;

	public ForwardBackwardBoth invert() {
		switch (this) {
		case FORWARD: return BACKWARD;
		case BACKWARD: return FORWARD;
		default: return BOTH;
		}
	}

	public static @Nullable ForwardBackwardBoth of(String s) {
		if (s == null) return null;
		try {
			return ForwardBackwardBoth.valueOf(s.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	public static ForwardBackwardBoth of(ForwardBackward value) {
		switch (requireNonNull(value)) {
		case FORWARD: return FORWARD;
		case BACKWARD: return BACKWARD;
		default: throw new Error("unknown value");
		}
	}

	public static @Nullable ForwardBackwardBoth ofNullable(@Nullable ForwardBackward value) {
		if (value == null) return null;
		switch (value) {
		case FORWARD: return FORWARD;
		case BACKWARD: return BACKWARD;
		default: throw new Error("unknown value");
		}
	}

}
