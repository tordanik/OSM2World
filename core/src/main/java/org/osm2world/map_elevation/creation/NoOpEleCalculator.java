package org.osm2world.map_elevation.creation;

import javax.annotation.Nonnull;

import org.osm2world.map_data.data.MapData;

/**
 * leaves existing elevations unmodified.
 * Usually, those existing elevations will be either all 0 or based on a terrain model.
 */
public class NoOpEleCalculator implements EleCalculator {

	@Override
	public void calculateElevations(@Nonnull MapData mapData) {}

}
