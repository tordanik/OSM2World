package org.osm2world.core.util;


public enum LeftRight {

	LEFT, RIGHT;

	public LeftRight invert() {
		return this == LEFT ? RIGHT : LEFT;
	}

}
