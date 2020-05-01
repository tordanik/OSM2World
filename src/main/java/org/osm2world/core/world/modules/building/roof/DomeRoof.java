package org.osm2world.core.world.modules.building.roof;

import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;

public class DomeRoof extends SpindleRoof {

	public DomeRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, double height, Material material) {
		super(originalPolygon, tags, height, material);
	}

	/**
	 * number of height rings to approximate the round dome shape
	 */
	private static final int HEIGHT_RINGS = 10;

	@Override
	public void renderTo(Target target, double baseEle) {

		List<Double> heights = new ArrayList<>();
		List<Double> scales = new ArrayList<>();

		for (int ring = 0; ring < HEIGHT_RINGS; ++ring) {
			double relativeHeight = (double)ring / (HEIGHT_RINGS - 1);
			heights.add(baseEle + relativeHeight * roofHeight);
			scales.add(sqrt(1.0 - relativeHeight * relativeHeight));
		}

		renderSpindle(target, material,
				getPolygon().getOuter().makeClockwise(),
				heights, scales);

	}

}