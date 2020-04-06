package org.osm2world.core.math;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
	public List<VectorXZ> getVertexList() {
		return outerPolygon.getVertexList();
	}

	public SimplePolygonXZ getOuter() {
		return outerPolygon;
	}

	@Override
	public List<SimplePolygonXZ> getHoles() {
		return holes;
	}

	@Override
	public List<SimplePolygonXZ> getPolygons() {
		if (getHoles().isEmpty()) {
			return singletonList(getOuter());
		} else {
			List<SimplePolygonXZ> result = new ArrayList<>(getHoles().size() + 1);
			result.add(getOuter());
			result.addAll(getHoles());
			return result;
		}
	}

	public TriangleXZ asTriangleXZ() {
		if (!holes.isEmpty()) {
			throw new InvalidGeometryException("polygon has holes, it cannot be used as a triangle");
		}
		return outerPolygon.asTriangleXZ();
	}


	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return getOuter().boundingBox();
	}

	public boolean contains(LineSegmentXZ lineSegment) {
		if (!this.contains(lineSegment.p1)
				|| !this.contains(lineSegment.p2)) {
			return false;
		} else {
			for (SimplePolygonXZ hole : holes) {
				if (hole.intersects(lineSegment.p1, lineSegment.p2)) {
					return false;
				}
			}
		}
		return true;
	}

	//TODO (duplicate code): do something like intersects(geometricObject)

	public boolean intersects(LineSegmentXZ lineSegment) {
		for (SimplePolygonXZ hole : holes) {
			if (hole.intersects(lineSegment)) {
				return true;
			}
		}
		return outerPolygon.intersects(lineSegment);
	}

	public boolean intersects(SimplePolygonXZ other) {
		for (SimplePolygonXZ hole : holes) {
			if (hole.intersects(other)) {
				return true;
			}
		}
		return outerPolygon.intersects(other);
	}

	public List<VectorXZ> intersectionPositions(LineSegmentXZ lineSegment) {
		List<VectorXZ> intersectionPositions = new ArrayList<VectorXZ>();
		for (SimplePolygonXZ hole : holes) {
			intersectionPositions.addAll(hole.intersectionPositions(lineSegment));
		}
		intersectionPositions.addAll(outerPolygon.intersectionPositions(lineSegment));
		return intersectionPositions;
	}

	public Collection<VectorXZ> intersectionPositions(PolygonWithHolesXZ p2) {
		List<VectorXZ> intersectionPositions = new ArrayList<VectorXZ>();
		for (SimplePolygonXZ simplePoly : p2.getPolygons()) {
			for (LineSegmentXZ lineSegment : simplePoly.getSegments()) {
				intersectionPositions.addAll(
						intersectionPositions(lineSegment));
			}
		}
		return intersectionPositions;
	}

	public double getArea() {
		//FIXME incorrect for overlapping holes
		double area = outerPolygon.getArea();
		for (SimplePolygonXZ hole : holes) {
			area -= hole.getArea();
		}
		return area;
	}

}
