package org.osm2world.core.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.math.shapes.PolygonShapeXZ;

public class PolygonWithHolesXZ implements PolygonShapeXZ {

	private final SimplePolygonXZ outerPolygon;
	private final List<SimplePolygonXZ> holes;

	public PolygonWithHolesXZ(SimplePolygonXZ outerPolygon,
			List<SimplePolygonXZ> holes) {
		this.outerPolygon = outerPolygon;
		this.holes = holes;
		/**
		 * forces the early computation of the area to avoid the creation of an invalid one. This is a temporary kludge as
		 * calling a method that can be overridden in a constructor is a bad practice. Moreover, the late call of the method
		 * calculateArea() was originally intended to avoid unnecessary computations but as it is necessary for intersection
		 * tests even before any rendering attempts, it no longer makes sense. A better solution would consist in running the
		 * computation of the area as soon as possible
		 * */
		this.getArea();
	}

	@Override
	public List<VectorXZ> vertices() {
		return outerPolygon.vertices();
	}

	@Override
	public SimplePolygonXZ getOuter() {
		return outerPolygon;
	}

	@Override
	public List<SimplePolygonXZ> getHoles() {
		return holes;
	}

	@Override
	public List<SimplePolygonXZ> getRings() {
		List<SimplePolygonXZ> result = new ArrayList<>(getHoles().size() + 1);
		result.add(getOuter());
		result.addAll(getHoles());
		return result;
	}

	public TriangleXZ asTriangleXZ() {
		if (!holes.isEmpty()) {
			throw new InvalidGeometryException("polygon has holes, it cannot be used as a triangle");
		}
		return outerPolygon.asTriangleXZ();
	}

	@Override
	public Collection<TriangleXZ> getTriangulation() {
		return TriangulationUtil.triangulate(this);
	}

	@Override
	public String toString() {
		return getRings().toString();
	}

}
