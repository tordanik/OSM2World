package org.osm2world.core.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PolygonWithHolesXZ {
	
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

	public SimplePolygonXZ getOuter() {
		return outerPolygon;
	}
	
	public List<SimplePolygonXZ> getHoles() {
		return holes;
	}
	
	/**
	 * returns a list that contains the outer polygon and all holes
	 */
	public List<SimplePolygonXZ> getPolygons() {
		if (holes.isEmpty()) {
			return Collections.singletonList(outerPolygon);
		} else {
			List<SimplePolygonXZ> result = new ArrayList<SimplePolygonXZ>(holes.size()+1);
			result.add(outerPolygon);
			result.addAll(holes);
			return result;
		}
	}

	public TriangleXZ asTriangleXZ() {
		if (!holes.isEmpty()) {
			throw new InvalidGeometryException("polygon has holes, it cannot be used as a triangle");
		}
		return outerPolygon.asTriangleXZ();
	}

	public boolean contains(SimplePolygonXZ boundary) {
		//FIXME currently returns true if boundary intersects one of the holes!
		if (!outerPolygon.contains(boundary)) {
			return false;
		} else {
			for (SimplePolygonXZ hole : holes) {
				if (hole.contains(boundary)) {
					return false;
				}
			}
			return true;
		}
	}

	public boolean contains(VectorXZ v) {
		if (!outerPolygon.contains(v)) {
			return false;
		} else {
			for (SimplePolygonXZ hole : holes) {
				if (hole.contains(v)) {
					return false;
				}
			}
			return true;
		}
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
