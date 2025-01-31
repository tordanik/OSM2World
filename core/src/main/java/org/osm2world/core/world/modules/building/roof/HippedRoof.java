package org.osm2world.core.world.modules.building.roof;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.Collection;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.LineSegmentXZ;
import org.osm2world.core.math.shapes.PolygonWithHolesXZ;
import org.osm2world.core.target.common.material.Material;

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
		if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) {
			return roofHeight;
		} else if (getPolygon().getOuter().getVertexCollection().contains(pos)) {
			return 0.0;
		} else {
			return null;
		}
	}

}