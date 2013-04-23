package org.osm2world.core.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.poly2tri.Poly2Tri;
import org.poly2tri.triangulation.Triangulatable;
import org.poly2tri.triangulation.TriangulationAlgorithm;
import org.poly2tri.triangulation.TriangulationContext;
import org.poly2tri.triangulation.TriangulationMode;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;
import org.poly2tri.triangulation.point.TPoint;
/**
 * @author Hannes Janetzek
 * */

public class Poly2TriUtil {
	static class CDTSet implements Triangulatable {
		List<TriangulationPoint> points = new ArrayList<TriangulationPoint>(20);
		List<DelaunayTriangle> triangles = new ArrayList<DelaunayTriangle>(20);

		ArrayList<LineSegmentXZ> segmentSet = new ArrayList<LineSegmentXZ>();

		public CDTSet(SimplePolygonXZ polygon,
				Collection<SimplePolygonXZ> holes,
				Collection<LineSegmentXZ> segments) {

			List<VectorXZ> vertices = polygon.getVertexLoop();

			segmentSet.addAll(segments);

			for (int i = 0, n = vertices.size() - 1; i < n; i++)
				segmentSet.add(new LineSegmentXZ(vertices.get(i),
						vertices.get(i + 1)));

			for (SimplePolygonXZ hole : holes) {
				vertices = hole.getVertexLoop();
				for (int i = 0, n = vertices.size() - 1; i < n; i++)
					segmentSet.add(new LineSegmentXZ(vertices.get(i),
							vertices.get(i + 1)));
			}

			removeDuplicateSegments();

			boolean foundIntersections = false;

			// split at intersections
			for (int i = 0, size = segmentSet.size(); i < size - 1; i++) {
				LineSegmentXZ l1 = segmentSet.get(i);

				for (int j = i + 1; j < size; j++) {
					LineSegmentXZ l2 = segmentSet.get(j);

					VectorXZ crossing;

					if ((crossing = l1.getIntersection(l2.p1, l2.p2)) != null) {
						System.out.println("split " + l1 + " " + l2 + " at "
								+ crossing);
						foundIntersections = true;

						segmentSet.remove(l1);
						segmentSet.remove(l2);

						segmentSet.add(new LineSegmentXZ(crossing, l1.p1));
						segmentSet.add(new LineSegmentXZ(crossing, l1.p2));
						segmentSet.add(new LineSegmentXZ(crossing, l2.p1));
						segmentSet.add(new LineSegmentXZ(crossing, l2.p2));

						size += 2;

						// first segment was removed
						i--;
						break;
					}
				}
			}

			if (foundIntersections)
				removeDuplicateSegments();
		}

		private void removeDuplicateSegments() {
			for (int i = 0, size = segmentSet.size(); i < size - 1; i++) {
				LineSegmentXZ l1 = segmentSet.get(i);

				for (int j = i + 1; j < size; j++) {
					LineSegmentXZ l2 = segmentSet.get(j);

					if ((l1.p1.equals(l2.p1) && l1.p2.equals(l2.p2))
							|| (l1.p1.equals(l2.p2) && l1.p2.equals(l2.p1))) {
						//System.out.println("remove dup " + l1 + " " + l2);
						segmentSet.remove(j);
						size--;
					}
				}
			}
		}

		public TriangulationMode getTriangulationMode() {
			return TriangulationMode.CONSTRAINED;
		}

		public List<TriangulationPoint> getPoints() {
			return points;
		}

		public List<DelaunayTriangle> getTriangles() {
			return triangles;
		}

		public void addTriangle(DelaunayTriangle t) {
			triangles.add(t);
		}

		public void addTriangles(List<DelaunayTriangle> list) {
			triangles.addAll(list);
		}

		public void clearTriangulation() {
			triangles.clear();
		}

		public void prepareTriangulation(TriangulationContext<?> tcx) {
			triangles.clear();

			// it seems poly2tri requires points to be unique objects
			HashMap<VectorXZ, TriangulationPoint> pointSet
				= new HashMap<VectorXZ, TriangulationPoint>();

			for (LineSegmentXZ l : segmentSet) {
				TriangulationPoint tp1, tp2;
				
				if (!pointSet.containsKey(l.p1)){
					tp1 = new TPoint(l.p1.x, l.p1.z);					
					pointSet.put(l.p1, tp1);
					points.add(tp1);
				} else {
					tp1 = pointSet.get(l.p1);
				}
				if (!pointSet.containsKey(l.p2)){
					tp2 = new TPoint(l.p2.x, l.p2.z);	
					pointSet.put(l.p2, tp2);
					points.add(tp2);
				} else {
					tp2 = pointSet.get(l.p2);
				}

				tcx.newConstraint(tp1, tp2);
			}

			segmentSet.clear();

			pointSet.clear();

			tcx.addPoints(points);
		}
	}

	public static final List<TriangleXZ> triangulate(SimplePolygonXZ polygon,
			Collection<SimplePolygonXZ> holes,
			Collection<LineSegmentXZ> segments, Collection<VectorXZ> points) {

		CDTSet cdt = new CDTSet(polygon, holes, segments);
		TriangulationContext<?> tcx = Poly2Tri
				.createContext(TriangulationAlgorithm.DTSweep);
		tcx.prepareTriangulation(cdt);

		Poly2Tri.triangulate(tcx);

		List<TriangleXZ> triangles = new ArrayList<TriangleXZ>();

		List<DelaunayTriangle> result = cdt.getTriangles();

		if (result == null)
			return triangles;

		for (DelaunayTriangle t : result) {

			TriangulationPoint tCenter = t.centroid();
			VectorXZ center = new VectorXZ(tCenter.getX(), tCenter.getY());

			boolean triangleInHole = false;
			for (SimplePolygonXZ hole : holes) {
				if (hole.contains(center)) {
					triangleInHole = true;
					break;
				}
			}

			if (triangleInHole || !polygon.contains(center))
				continue;

			triangles.add(new TriangleXZ(new VectorXZ(t.points[0].getX(),
					t.points[0].getY()), new VectorXZ(t.points[1].getX(),
					t.points[1].getY()), new VectorXZ(t.points[2].getX(),
					t.points[2].getY())));
		}

		return triangles;

	}
}