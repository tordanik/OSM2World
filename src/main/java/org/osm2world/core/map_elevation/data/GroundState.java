package org.osm2world.core.map_elevation.data;

public enum GroundState {

	ON, ABOVE, BELOW;

	public boolean isHigherThan(GroundState other) {
		return this == ABOVE && other != ABOVE
			|| this == ON && other == BELOW;
	}

}