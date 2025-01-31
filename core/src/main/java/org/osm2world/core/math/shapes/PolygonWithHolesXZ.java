package org.osm2world.core.math.shapes;

import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.shapes.SimplePolygonXZ.asSimplePolygon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.util.exception.InvalidGeometryException;

public class PolygonWithHolesXZ implements PolygonShapeXZ {

	private final SimplePolygonXZ outerPolygon;
	private final List<SimplePolygonXZ> holes;

	public PolygonWithHolesXZ(SimplePolygonXZ outerPolygon,
			List<SimplePolygonXZ> holes) {
		this.outerPolygon = outerPolygon;
		this.holes = holes;
		// calculate the area in the constructor to catch degenerate polygons early
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
	public List<TriangleXZ> getTriangulation() {
		if (holes.isEmpty()) {
			return outerPolygon.getTriangulation();
		} else {
			return TriangulationUtil.triangulate(this);
		}
	}

	@Override
	public PolygonWithHolesXZ transform(Function<VectorXZ, VectorXZ> operation) {
		return new PolygonWithHolesXZ(
				asSimplePolygon(getOuter().transform(operation)),
				getHoles().stream().map(it -> asSimplePolygon(it.transform(operation))).collect(toList()));
	}

	public PolygonWithHolesXYZ xyz(double y) {
		return new PolygonWithHolesXYZ(getOuter().xyz(y),
				getHoles().stream().map(it -> it.xyz(y)).toList());
	}

	@Override
	public String toString() {
		return getRings().toString();
	}

}
