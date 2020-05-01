package org.osm2world.core.world.modules.building.roof;

import static java.util.Collections.emptyList;

import java.util.Collection;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.Material;

public class FlatRoof extends HeightfieldRoof {

	public FlatRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(originalPolygon, tags, 0, material);
	}

	@Override
	public PolygonWithHolesXZ getPolygon() {
		return originalPolygon;
	}

	@Override
	public Collection<VectorXZ> getInnerPoints() {
		return emptyList();
	}

	@Override
	public Collection<LineSegmentXZ> getInnerSegments() {
		return emptyList();
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {
		return 0.0;
	}

}