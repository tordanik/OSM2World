package org.osm2world.world.modules.building.roof;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.osm2world.math.algorithms.GeometryUtil.distanceFromLineSegment;

import java.util.Collection;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;

public class HippedRoof extends RoofWithRidge {

	public HippedRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(1/3.0, originalPolygon, tags, material);
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
		return asList(
				ridge,
				new LineSegmentXZ(ridge.p1, cap1.p1),
				new LineSegmentXZ(ridge.p1, cap1.p2),
				new LineSegmentXZ(ridge.p2, cap2.p1),
				new LineSegmentXZ(ridge.p2, cap2.p2));
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {
		double distRidge = distanceFromLineSegment(pos, ridge);
		double relativePlacement = distRidge / maxDistanceToRidge;
		return roofHeight - roofHeight * relativePlacement;
	}

}