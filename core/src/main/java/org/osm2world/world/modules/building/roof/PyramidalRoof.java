package org.osm2world.world.modules.building.roof;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.target.common.material.Material;

public class PyramidalRoof extends HeightfieldRoof {

	private final VectorXZ apex;
	private final List<LineSegmentXZ> innerSegments;

	public PyramidalRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {

		super(originalPolygon, tags, material);

		SimplePolygonXZ outerPoly = originalPolygon.getOuter();

		apex = outerPoly.getCentroid();

		innerSegments = new ArrayList<>();
		for (VectorXZ v : outerPoly.getVertices()) {
			innerSegments.add(new LineSegmentXZ(v, apex));
		}

	}

	@Override
	public PolygonWithHolesXZ getPolygon() {
		return originalPolygon;
	}

	@Override
	public Collection<VectorXZ> getInnerPoints() {
		return singletonList(apex);
	}

	@Override
	public Collection<LineSegmentXZ> getInnerSegments() {
		return innerSegments;
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {
		if (apex.equals(pos)) {
			return roofHeight;
		} else if (originalPolygon.getOuter().getVertices().contains(pos)) {
			return 0.0;
		} else {
			return null;
		}
	}

}