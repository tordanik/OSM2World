package org.osm2world.core.world.modules.building.roof;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.osm2world.core.math.GeometryUtil.*;

import java.util.Collection;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.PolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.Material;

public class HalfHippedRoof extends RoofWithRidge {

	private final LineSegmentXZ cap1part, cap2part;

	public HalfHippedRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, double height, Material material) {

		super(1/6.0, originalPolygon, tags, height, material);

		cap1part = new LineSegmentXZ(
				interpolateBetween(cap1.p1, cap1.p2, 0.5 - ridgeOffset / cap1.getLength()),
				interpolateBetween(cap1.p1, cap1.p2, 0.5 + ridgeOffset / cap1.getLength()));

		cap2part = new LineSegmentXZ(
				interpolateBetween(cap2.p1, cap2.p2, 0.5 - ridgeOffset / cap1.getLength()),
				interpolateBetween(cap2.p1, cap2.p2, 0.5 + ridgeOffset / cap1.getLength()));

	}

	@Override
	public PolygonWithHolesXZ getPolygon() {

		PolygonXZ newOuter = originalPolygon.getOuter();

		newOuter = insertIntoPolygon(newOuter, cap1part.p1, 0.2);
		newOuter = insertIntoPolygon(newOuter, cap1part.p2, 0.2);
		newOuter = insertIntoPolygon(newOuter, cap2part.p1, 0.2);
		newOuter = insertIntoPolygon(newOuter, cap2part.p2, 0.2);

		return new PolygonWithHolesXZ(newOuter.asSimplePolygon(), originalPolygon.getHoles());

	}

	@Override
	public Collection<VectorXZ> getInnerPoints() {
		return emptyList();
	}

	@Override
	public Collection<LineSegmentXZ> getInnerSegments() {
		return asList(ridge,
				new LineSegmentXZ(ridge.p1, cap1part.p1),
				new LineSegmentXZ(ridge.p1, cap1part.p2),
				new LineSegmentXZ(ridge.p2, cap2part.p1),
				new LineSegmentXZ(ridge.p2, cap2part.p2));
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {
		if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) {
			return roofHeight;
		} else if (getPolygon().getOuter().getVertexCollection().contains(pos)) {
			if (distanceFromLineSegment(pos, cap1part) < 0.05) {
				return roofHeight - roofHeight * ridgeOffset / (cap1.getLength()/2);
			} else if (distanceFromLineSegment(pos, cap2part) < 0.05) {
				return roofHeight - roofHeight * ridgeOffset / (cap2.getLength()/2);
			} else {
				return 0.0;
			}
		} else {
			return null;
		}
	}

}