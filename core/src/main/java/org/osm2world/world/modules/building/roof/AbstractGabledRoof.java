package org.osm2world.world.modules.building.roof;

import static org.osm2world.math.algorithms.GeometryUtil.distanceFromLineSegment;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

public abstract class AbstractGabledRoof extends RoofWithRidge {

	public AbstractGabledRoof(@Nullable BuildingPart buildingPart, @Nullable Double relativeRidgeLocation,
			PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(buildingPart, 0, 0, false, relativeRidgeLocation,
				originalPolygon, tags, material);
	}

	@Override
	protected Collection<InnerLine> getInnerLines() {
		return List.of(new InnerLine(ridge, true));
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {
		double distRidge = distanceFromLineSegment(pos, ridge);
		double relativePlacement = distRidge / maxDistanceToRidge;
		return roofHeight() - roofHeight() * relativePlacement;
	}

}