package org.osm2world.world.modules.building.roof;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.target.common.material.Material;

public class OnionRoof extends SpindleRoof {

	public OnionRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(originalPolygon, tags, material);
	}

	@Override
	protected List<Pair<Double, Double>> getSpindleSteps() {
		return asList(
				Pair.of(0.0, 1.0),
				Pair.of(0.15, 0.8),
				Pair.of(0.52, 1.0),
				Pair.of(0.72, 0.7),
				Pair.of(0.82, 0.15),
				Pair.of(1.00, 0.0));
	}

}