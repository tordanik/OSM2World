package org.osm2world.world.modules.building.roof;

import static java.util.Collections.emptyList;

import java.util.Collection;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.output.common.material.Material;

public class FlatRoof extends HeightfieldRoof {

	public FlatRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(originalPolygon, tags, material);
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
	public Double calculatePreliminaryHeight() {
		return 0.0;
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {
		return 0.0;
	}

}
