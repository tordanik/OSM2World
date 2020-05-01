package org.osm2world.core.world.modules.building.roof;

import static java.util.Arrays.asList;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;

public class OnionRoof extends SpindleRoof {

	public OnionRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, double height, Material material) {
		super(originalPolygon, tags, height, material);
	}

	@Override
	public void renderTo(Target target, double baseEle) {

		renderSpindle(target, material,
				getPolygon().getOuter().makeClockwise(),
				asList(baseEle,
						baseEle + 0.15 * roofHeight,
						baseEle + 0.52 * roofHeight,
						baseEle + 0.72 * roofHeight,
						baseEle + 0.82 * roofHeight,
						baseEle + 1.00 * roofHeight),
				asList(1.0, 0.8, 1.0, 0.7, 0.15, 0.0));

	}

}