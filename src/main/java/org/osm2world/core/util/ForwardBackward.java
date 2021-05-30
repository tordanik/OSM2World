package org.osm2world.core.util;


public enum ForwardBackward {

	FORWARD, BACKWARD;

	public ForwardBackward invert() {
		return this == FORWARD ? BACKWARD : FORWARD;
	}

}
