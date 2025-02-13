package org.osm2world.world.modules.building.roof;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.osm2world.math.algorithms.GeometryUtil.distanceFromLineSegment;
import static org.osm2world.math.algorithms.GeometryUtil.insertIntoPolygon;

import java.util.Collection;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.output.common.material.Material;

public class GabledRoof extends RoofWithRidge {

	public GabledRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(0, originalPolygon, tags, material);
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