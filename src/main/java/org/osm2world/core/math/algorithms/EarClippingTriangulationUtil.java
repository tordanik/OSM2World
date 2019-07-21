package org.osm2world.core.math.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;

public final class EarClippingTriangulationUtil {

	private EarClippingTriangulationUtil() { }

	/**
	 * triangulates a two-dimensional polygon
	 * by creating a simple polygon first
	 * (integrating holes into the polygon outline),
	 * then using Ear Clipping on that simple polygon.
	 */
	public static final List<TriangleXZ> triangulate(
			SimplePolygonXZ polygon,
			Collection<SimplePolygonXZ> holes) {

		/*
		 * store the outline of the polygon;
		 * additional vertices from holes and terrain will be integrated later.
		 * Use LinkedList for insertion performance.
		 */

		List<VectorXZ> polygonOutline = new LinkedList<VectorXZ>(polygon.getVertexLoop());

		/* integrate holes */

		insertHolesInPolygonOutline(polygonOutline, holes);

//		if (simplify) {
//		 * @param simplify  if true, the polygon will be modified in a way
//		 *                  that improves the robustness of the algorithm,
//		 *                  but does not preserve the original shape entirely.
//		 *                  This should be used if a first attempt to triangulate failed.
//			simplifyOutline(polygonOutline);
//		}

		/* triangulate */

		if (polygonOutline.size() >= 3) {
			return triangulateSimplePolygon(polygonOutline);
		} else {
			return Collections.emptyList();
		}

	}

	/**
	 * inserts multiple holes into a polygon outline using
	 * {@link #insertHoleInPolygonOutline(List, SimplePolygonXZ, List)}
	 *
	 * TODO: public for debugging purposes
	 */
	public static void insertHolesInPolygonOutline(
			List<VectorXZ> polygonOutline, Collection<SimplePolygonXZ> holes) {

		List<SimplePolygonXZ> remainingHoles = new LinkedList<SimplePolygonXZ>(holes);

		//It is possible that some holes block other holes' access to the outline.
		//However, at least one hole will be inserted during each loop execution.
		while (!remainingHoles.isEmpty()) {
			for (Iterator<SimplePolygonXZ> holeIt = remainingHoles.iterator(); holeIt.hasNext(); ) {
				SimplePolygonXZ hole = holeIt.next();
				boolean success = insertHoleInPolygonOutline(polygonOutline, hole, remainingHoles);
				if (success) {
					holeIt.remove();
				}
			}
		}
	}

	/**
	 * @param polygonOutline  polygon outline; will be modified directly to perform the insertion
	 * @param hole   hole to be inserted
	 * @param holes  all holes that haven't been integrated into the polygon yet, including 'hole'
	 * @return       true if inserting the hole was successful
	 */
	static final boolean insertHoleInPolygonOutline(
			List<VectorXZ> polygonOutline, SimplePolygonXZ hole, List<SimplePolygonXZ> holes) {

		int innerIndex;
		Integer outerIndex = null;

		List<VectorXZ> holeVertices = hole.getVertices();

		for (innerIndex = 0; innerIndex < holeVertices.size(); innerIndex++) {
			outerIndex = findVisibleOutlineVertex(polygonOutline,
					holeVertices.get(innerIndex), holes);
			if (outerIndex != null) { break; }
		}

		if (outerIndex == null) {
			return false;
		} else {

			SimplePolygonXZ outerPolygon = new SimplePolygonXZ(new ArrayList<VectorXZ>(polygonOutline)); //TODO: avoid creating this every time

			polygonOutline.add(outerIndex+1, polygonOutline.get(outerIndex));

			if (hole.isClockwise() ^ outerPolygon.isClockwise()) {
				polygonOutline.addAll(outerIndex+1,
						rearrangeOutline(hole.getVertexLoop(), innerIndex, false));
			} else {
				polygonOutline.addAll(outerIndex+1,
						rearrangeOutline(hole.getVertexLoop(), innerIndex, true));
			}

			return true;

		}

	}

	/**
	 * rearranges a polygon outline.
	 *
	 * @param outline  old polygon outline, will not be modified.
	 *                 First and last vertex need to be identical.
	 * @param newStart index of the new first/last outline vertex
	 * @param invert   determines whether the order of vertices is inverted
	 * @return  New polygon outline.
	 * 			The new first and last vertex are identical.
	 *
	 */
	static final List<VectorXZ> rearrangeOutline(
			List<VectorXZ> outline, int newStart, boolean invert) {

		List<VectorXZ> newOutline = new ArrayList<VectorXZ>(outline.size());

		boolean complete = false;
		boolean oldStartAdded = false;
		int currentIndex = newStart;

		while (!complete) {

			/* add vertex to new outline */

			if (currentIndex == 0 || currentIndex == outline.size() - 1) {
				if (!oldStartAdded) {//remove original duplication of first/last node
					newOutline.add(outline.get(0));
					oldStartAdded = true;
				}
			} else {
				newOutline.add(outline.get(currentIndex));
			}

			/* shift current index */

			if (!invert) {
				currentIndex += 1;
				if (currentIndex >= outline.size()) {
					currentIndex = 0;
				}
			} else {
				currentIndex -= 1;
				if (currentIndex < 0) {
					currentIndex = outline.size() - 1;
				}
			}

			if (currentIndex == newStart) {
				complete = true;
			}

		}

		newOutline.add(outline.get(newStart));

		return newOutline;
	}

	static final void insertVertexInPolygonOutline(
			List<VectorXZ> polygonOutline, VectorXZ point) {

		int index = findVisibleOutlineVertex(polygonOutline, point,
				Collections.<SimplePolygonXZ>emptyList());

		polygonOutline.add(index+1, point);
		polygonOutline.add(index+2, polygonOutline.get(index));

	}

	/**
	 * finds a vertex in the polygon outline that is visible from a given point.
	 * Visibility means that the connection between the point and the outline
	 * vertex does not intersect the outline or any hole polygon.
	 *
	 * @return  index of a vertex in the polygon outline
	 *          or null if none was found.
	 *          The method will always find an outline vertex
	 *          if there are no holes.
	 */
	static final Integer findVisibleOutlineVertex(
			List<VectorXZ> polygonOutline, VectorXZ point,
			Iterable<SimplePolygonXZ> holes) {

		// TODO (performance): replace primitive algorithm with more efficient one

		int outerIndex = -1;

		checkOuterVertex:
		for (VectorXZ outerVertex : polygonOutline) {

			outerIndex += 1;

			for (int i=0; i+1 < polygonOutline.size(); i++) {
				if (null != GeometryUtil.getTrueLineSegmentIntersection(
						point, outerVertex,
						polygonOutline.get(i), polygonOutline.get(i+1))) {
					continue checkOuterVertex;
				}
			}

			for (SimplePolygonXZ hole : holes) {
				if (hole.intersects(point, outerVertex)) {
					continue checkOuterVertex;
				}
			}

			return outerIndex;

		}

		return null;

	}

	/**
	 * "simplifies" the outline by removing vertices that are (almost)
	 * on a straight edge, or very close to each other,
	 * because the triangulation method cannot handle these situations
	 *  - they produce zero-area polygons.
	 *
	 * Using this method <em>does</em> affect the result, especially if the
	 * elevation data associated with the eliminated vertices isn't properly
	 * represented by linear interpolation between the adjacent vertices.
	 */
	private static void simplifyOutline(List<VectorXZ> outline) {

		outline.remove(outline.size()-1);

		Set<Integer> removeIndizes = new HashSet<Integer>();

		/* find vertices on straight lines */

		for (int i = 0; i < outline.size(); i++) {
			if (GeometryUtil.distanceFromLineSegment(
				outline.get(i),
				new LineSegmentXZ(vertexBefore(outline, i),
					vertexAfter(outline, i))) < 0.001) {
				removeIndizes.add(i);
			}
		}

//		these didn't have a noticeable effect yet
//
//		/* remove two vertices if they are very close and
//		 * also close to each other's position in the sequence */
//
//		for (int i = 0; i < outline.size(); i++) {
//			if (VectorXZ.distance(outline.get(i),
//					vertexAfter(outline, i)) < 0.001) {
//				removeIndizes.add(i);
//			}
//		}
//
//		for (int i = 0; i < outline.size(); i++) {
//			if (VectorXZ.distance(vertexBefore(outline, i),
//					vertexAfter(outline, i)) < 0.001) {
//				removeIndizes.add(indexBefore(outline, i));
//				removeIndizes.add(i);
//			}
//		}

		/* actually remove the vertices */

		for (int i = outline.size(); i >= 0; i--) {
			if (removeIndizes.contains(i)) {
				System.out.println("simplify removes vertex #" + i + " in outline: " + outline);
				outline.remove(i);
			}
		}

		/* restore the polygon loop property */

		outline.add(outline.get(0));

	}

	/**
	 * Triangulates a simple polygon using Ear Clipping.
	 * The implementation is based on the paper
	 * "Triangulation by Ear Clipping" by David Eberly.
	 *
	 * @param outline  outline of the polygon to triangulate;
	 *                 can be arbitrarily modified by this method.
	 */
	static final List<TriangleXZ> triangulateSimplePolygon(
			List<VectorXZ> outline) {

		if (outline.size() == 3) {
			return Collections.singletonList(
					new TriangleXZ(outline.get(1), outline.get(2), outline.get(3)));
		}

		outline.remove(0); //TODO: only while first/last vertex is still duplicated

		List<TriangleXZ> triangles = new ArrayList<TriangleXZ>(outline.size() - 2);

		boolean progress = true;

		while(outline.size() >= 3 && progress) {
			progress = false;
			for (int i=0; i < outline.size(); i++) {
				if (isEarTip(i, outline)) {
					triangles.add(triangleAtTip(i, outline));
					outline.remove(i);
					progress = true;
					break;
				}
			}
		}

		if (outline.size() >= 3) {
			throw new InvalidGeometryException("failed to triangulate outline."
					+ "\nRemaining: " + outline
					+ "\nTriangles: " + triangles);
		}

//		TODO: try to remove the need for progress check
//      TODO (performance) use better algorithm instead of the n^3 above

		return triangles;

	}

	static boolean isEarTip(int i, List<VectorXZ> outline) {

		if (isConvex(i, outline)) {

			TriangleXZ triangleAtTip = triangleAtTip(i, outline);

			for (VectorXZ vertex : outline) {
				if (vertex != triangleAtTip.v1
						&& vertex != triangleAtTip.v2
						&& vertex != triangleAtTip.v3
						&& triangleAtTip.contains(vertex)) {
					return false;
				}
			}

			return true;

		}

		return false;

	}

	/**
	 * //TODO: outline expects NO duplication of first/last node
	 */
	static boolean isConvex(int i, List<VectorXZ> outline) {

		List<VectorXZ> tempVertices = new ArrayList<VectorXZ>(outline);
		tempVertices.add(outline.get(0));
		SimplePolygonXZ tempPolygon = new SimplePolygonXZ(tempVertices);

		//TODO (performance) avoid creating polygon by passing clockwise information

		VectorXZ segBefore = outline.get(i).subtract(vertexBefore(outline, i));
		VectorXZ segAfter = vertexAfter(outline, i).subtract(outline.get(i));

		return tempPolygon.isClockwise() ^
			segBefore.z * segAfter.x - segBefore.x * segAfter.z < 0;

	}

	static final TriangleXZ triangleAtTip(int i, List<VectorXZ> outline) {

		return new TriangleXZ(vertexBefore(outline, i),
				outline.get(i), vertexAfter(outline, i));

	}

	private static final VectorXZ vertexBefore(List<VectorXZ> outline, int i) {
		int beforeI = indexBefore(outline, i);
		return outline.get(beforeI);
	}

	private static int indexBefore(List<VectorXZ> outline, int i) {
		return (outline.size() + i - 1) % outline.size();
	}

	private static final VectorXZ vertexAfter(List<VectorXZ> outline, int i) {
		int afterI = indexAfter(outline, i);
		return outline.get(afterI);
	}

	private static int indexAfter(List<VectorXZ> outline, int i) {
		return (i + 1) % outline.size();
	}

	/**
	 * TODO (documentation)
	 */
	static final Collection<SimplePolygonXZ> splitAlong(SimplePolygonXZ splitPolygon,
			Collection<VectorXZ[]> splitLines) {

		Collection<SimplePolygonXZ> polygons = new ArrayList<SimplePolygonXZ>(1);
		polygons.add(splitPolygon);

		for (VectorXZ[] splitLine : splitLines) {

			Collection<SimplePolygonXZ> newPolygons = new ArrayList<SimplePolygonXZ>(polygons.size());

			for (SimplePolygonXZ polygon : polygons) {

				Map<Integer, VectorXZ> intersectionPos;
				Map<Integer, LineSegmentXZ> intersectionPartner;

				for (int lineVertexI = 0; lineVertexI + 1 < splitLine.length; lineVertexI++) {

				}

				//TODO: implement (but check whether this method is actually needed first)

			}

			polygons = newPolygons;

		}

		return polygons;

	}

}
