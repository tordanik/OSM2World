package org.osm2world.core.world.modules.building.roof;

import static java.util.Collections.*;
import static org.osm2world.core.math.GeometryUtil.*;

import java.util.Collection;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.Material;

public class GabledRoof extends RoofWithRidge {

	public GabledRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, double height, Material material) {
		super(0, originalPolygon, tags, height, material);
	}

	@Override
	public PolygonWithHolesXZ getPolygon() {

		SimplePolygonXZ newOuter = originalPolygon.getOuter();

		newOuter = insertIntoPolygon(newOuter, ridge.p1, SNAP_DISTANCE);
		newOuter = insertIntoPolygon(newOuter, ridge.p2, SNAP_DISTANCE);

		return new PolygonWithHolesXZ(newOuter, originalPolygon.getHoles());

	}

	@Override
	public Collection<VectorXZ> getInnerPoints() {
		return emptyList();
	}

	@Override
	public Collection<LineSegmentXZ> getInnerSegments() {
		return singleton(ridge);
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {
		double distRidge = distanceFromLineSegment(pos, ridge);
		double relativePlacement = distRidge / maxDistanceToRidge;
		return roofHeight - roofHeight * relativePlacement;
	}

}