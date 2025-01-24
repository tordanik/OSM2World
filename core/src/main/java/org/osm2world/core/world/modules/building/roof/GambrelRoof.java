package org.osm2world.core.world.modules.building.roof;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.osm2world.core.math.GeometryUtil.*;

import java.util.Collection;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.Material;

public class GambrelRoof extends RoofWithRidge {

	private final LineSegmentXZ cap1part, cap2part;

	public GambrelRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {

		super(0, originalPolygon, tags, material);

		cap1part = new LineSegmentXZ(
				interpolateBetween(cap1.p1, cap1.p2, 1/6.0),
				interpolateBetween(cap1.p1, cap1.p2, 5/6.0));

		cap2part = new LineSegmentXZ(
				interpolateBetween(cap2.p1, cap2.p2, 1/6.0),
				interpolateBetween(cap2.p1, cap2.p2, 5/6.0));

	}

	@Override
	public PolygonWithHolesXZ getPolygon() {

		SimplePolygonXZ newOuter = originalPolygon.getOuter();

		newOuter = insertIntoPolygon(newOuter, ridge.p1, SNAP_DISTANCE);
		newOuter = insertIntoPolygon(newOuter, ridge.p2, SNAP_DISTANCE);
		newOuter = insertIntoPolygon(newOuter, cap1part.p1, SNAP_DISTANCE);
		newOuter = insertIntoPolygon(newOuter, cap1part.p2, SNAP_DISTANCE);
		newOuter = insertIntoPolygon(newOuter, cap2part.p1, SNAP_DISTANCE);
		newOuter = insertIntoPolygon(newOuter, cap2part.p2, SNAP_DISTANCE);

		//TODO: add intersections of additional edges with outline?

		return new PolygonWithHolesXZ(newOuter, originalPolygon.getHoles());

	}

	@Override
	public Collection<VectorXZ> getInnerPoints() {
		return emptyList();
	}

	@Override
	public Collection<LineSegmentXZ> getInnerSegments() {
		return asList(ridge,
				new LineSegmentXZ(cap1part.p1, cap2part.p2),
				new LineSegmentXZ(cap1part.p2, cap2part.p1));
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {

		double distRidge = distanceFromLineSegment(pos, ridge);
		double relativePlacement = distRidge / maxDistanceToRidge;

		if (relativePlacement < 2/3.0) {
			return roofHeight - 1/2.0 * roofHeight * relativePlacement;
		} else {
			return roofHeight - 1/3.0 * roofHeight - 2 * roofHeight * (relativePlacement - 2/3.0);
		}

	}

}