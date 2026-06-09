package org.osm2world.world.modules.building.roof;

import static java.util.Collections.emptyList;

import java.util.Collection;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

public class FlatRoof extends HeightfieldRoof {

	public FlatRoof(@Nullable BuildingPart buildingPart, PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(buildingPart, originalPolygon, tags, material);
	}

	@Override
	public PolygonWithHolesXZ getPolygon() {
		return originalPolygon;
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
