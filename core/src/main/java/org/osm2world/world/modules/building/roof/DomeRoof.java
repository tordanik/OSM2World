package org.osm2world.world.modules.building.roof;

import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;

public class DomeRoof extends SpindleRoof {

	public DomeRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(originalPolygon, tags, material);
	}

	/**
	 * number of height rings to approximate the round dome shape
	 */
	private static final int HEIGHT_RINGS = 10;

	@Override
	protected List<Pair<Double, Double>> getSpindleSteps() {

		List<Pair<Double, Double>> steps = new ArrayList<>();

		for (int ring = 0; ring < HEIGHT_RINGS; ++ring) {
			double relativeHeight = ((double)ring) / (HEIGHT_RINGS - 1);
			steps.add(Pair.of(relativeHeight, sqrt(1.0 - relativeHeight * relativeHeight)));
		}

		return steps;

	}

}