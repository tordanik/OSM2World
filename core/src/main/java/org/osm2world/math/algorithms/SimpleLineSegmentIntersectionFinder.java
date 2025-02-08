package org.osm2world.math.algorithms;

import static org.osm2world.math.algorithms.GeometryUtil.getTrueLineSegmentIntersection;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.LineSegmentIntersectionFinder.Intersection;
import org.osm2world.math.shapes.LineSegmentXZ;

/** a simple but slow alternative to {@link LineSegmentIntersectionFinder} */
public class SimpleLineSegmentIntersectionFinder {

	/**
	 * finds all intersections in a set of line segments.
	 * Only reports true intersections, not shared start or end points.
	 */
	public static final List<Intersection<LineSegmentXZ>> findAllIntersections(List<? extends LineSegmentXZ> segments) {

		List<Intersection<LineSegmentXZ>> result = new ArrayList<>();

		for (int i = 0; i < segments.size(); i++) {
			for (int j = i + 1; j < segments.size(); j++) {
				LineSegmentXZ segI = segments.get(i);
				LineSegmentXZ segJ = segments.get(j);
				VectorXZ intersectionPos = getTrueLineSegmentIntersection(segI.p1, segI.p2, segJ.p1, segJ.p2);
				if (intersectionPos != null) {
					result.add(new Intersection<LineSegmentXZ>(intersectionPos, segI, segJ));
				}
			}
		}

		return result;

	}

}
