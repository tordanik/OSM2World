package org.osm2world.core.math.algorithms;

import static java.util.Collections.min;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.LineSegmentIntersectionFinder.Intersection;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.ShapeXZ;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/** utilities for finding the faces in a graph of line segments */
public final class FaceDecompositionUtil {

	private static final Comparator<VectorXZ> X_Y_COMPARATOR =
			Comparator.comparingDouble((VectorXZ v) -> v.x).thenComparingDouble(v -> v.z);

	private FaceDecompositionUtil() {}

	public static final Collection<PolygonWithHolesXZ> splitPolygonIntoFaces(PolygonShapeXZ polygon,
			Iterable<? extends ShapeXZ> otherShapes) {

		List<LineSegmentXZ> segments = new ArrayList<>();
		polygon.getRings().forEach(r -> segments.addAll(r.getSegments()));
		otherShapes.forEach(s -> segments.addAll(s.getSegments()));

		Collection<PolygonWithHolesXZ> result = facesFromGraph(segments);
		result.removeIf(p -> !polygon.contains(p.getPointInside()));
		return result;

	}

	public static final Collection<PolygonWithHolesXZ> facesFromGraph(List<LineSegmentXZ> segments) {

		//TODO consider applying a global precision model to VectorXZ instead of these piecemeal solutions
		final double SNAP_DISTANCE = 1e-5;

		Set<LineSegmentXZ> edges = new HashSet<>();

		/* determine all intersection points */

		//TODO: use the faster LineSegmentIntersectionFinder once it is bug-free
		List<Intersection<LineSegmentXZ>> intersections = SimpleLineSegmentIntersectionFinder.findAllIntersections(segments);

		/* snap intersection points to nearby segment endpoints */

		Set<VectorXZ> knownPoints = segments.stream().flatMap(s -> s.getVertexList().stream()).collect(toSet());
		List<Intersection<LineSegmentXZ>> newIntersections = new ArrayList<>();
		for (Iterator<Intersection<LineSegmentXZ>> iterator = intersections.iterator(); iterator.hasNext();) {
			Intersection<LineSegmentXZ> intersection = iterator.next();
			VectorXZ closestKnownPoint = knownPoints.stream().min(comparingDouble(intersection.pos::distanceTo)).get();
			if (closestKnownPoint.distanceTo(intersection.pos) < SNAP_DISTANCE) {
				iterator.remove();
				newIntersections.add(new Intersection<>(closestKnownPoint, intersection.segmentA, intersection.segmentB));
			}
		}
		intersections.addAll(newIntersections);

		/* split the segments at the intersection points (also deduplicates edges) */

		Multimap<LineSegmentXZ, VectorXZ> intersectionPointsPerSegment = HashMultimap.create(); //deduplicates values
		for (Intersection<LineSegmentXZ> intersection : intersections) {
			intersectionPointsPerSegment.put(intersection.segmentA, intersection.pos);
			intersectionPointsPerSegment.put(intersection.segmentB, intersection.pos);
		}
		for (LineSegmentXZ segment : segments) {
			intersectionPointsPerSegment.putAll(segment, segment.getVertexList());
		}

		for (LineSegmentXZ segment : intersectionPointsPerSegment.keys()) {

			List<VectorXZ> points = new ArrayList<>(intersectionPointsPerSegment.get(segment));

			VectorXZ start = min(segment.getVertexList(), X_Y_COMPARATOR);
			points.sort(Comparator.comparingDouble(p -> p.distanceTo(start)));

			for (int i = 0; i + 1 < points.size(); i++) {
				edges.add(new LineSegmentXZ(points.get(i), points.get(i + 1)));
			}

		}

		/* create a set of Nodes from the segments' end points */

		Set<VectorXZ> nodes = edges.stream().flatMap(e -> e.getVertexList().stream()).collect(toSet());

		/* calculate and return the result */

		return facesFromFullyNodedGraph(nodes, edges);

	}

	/**
	 * implementation of {@link #facesFromGraph(List)},
	 * requires a fully noded graph with duplicate-free undirected edges as input
	 */
	private static final Collection<PolygonWithHolesXZ> facesFromFullyNodedGraph(Collection<VectorXZ> nodes,
			Collection<LineSegmentXZ> undirectedEdges) {

		List<LineSegmentXZ> directedEdges = new ArrayList<>(undirectedEdges.size() * 2);
		for (LineSegmentXZ edge : undirectedEdges) {
			directedEdges.add(edge);
			directedEdges.add(edge.reverse());
		}

		/* create the ordered (clockwise) list of outgoing edges at each node */

		Map<VectorXZ, List<LineSegmentXZ>> outgoingEdgesForNodes = new HashMap<>();

		for (VectorXZ node : nodes) {

			List<LineSegmentXZ> outgoingEdges = new ArrayList<>();

			for (LineSegmentXZ edge : directedEdges) {
				if (edge.p1.equals(node)) {
					outgoingEdges.add(edge);
				}
			}

			outgoingEdges.sort(Comparator.comparingDouble(e -> e.getDirection().angle()));

			outgoingEdgesForNodes.put(node, outgoingEdges);

		}

		/*
		 * perform the algorithm:
		 * - start with any edge
		 * - construct the counterclockwise face containing it
		 *   (the infinite face on the outside, and inners, are clockwise)
		 * - use a set to manage not yet visited edges
		 */

		Set<LineSegmentXZ> remainingEdges = new HashSet<>(directedEdges);
		List<SimplePolygonXZ> faces = new ArrayList<>();

		while (!remainingEdges.isEmpty()) {

			LinkedList<LineSegmentXZ> currentPath = new LinkedList<>();
			currentPath.add(remainingEdges.iterator().next());

			while (!currentPath.getFirst().equals(currentPath.getLast()) || currentPath.size() == 1) {

				LineSegmentXZ currentEdge = currentPath.getLast();

				List<LineSegmentXZ> outgoingEdges = outgoingEdgesForNodes.get(currentEdge.p2);

				int incomingIndex = outgoingEdges.indexOf(currentEdge.reverse());
				int outgoingIndex = (incomingIndex + 1) % outgoingEdges.size();

				currentPath.add(outgoingEdges.get(outgoingIndex));

			}

			remainingEdges.removeAll(currentPath);

			List<VectorXZ> vertexLoop = currentPath.stream().map(e -> e.p1).collect(toList());
			faces.add(new SimplePolygonXZ(vertexLoop));

		}

		/* try to fit clockwise polygons into counterclockwise ones as inners */

		List<SimplePolygonXZ> outerRings = faces.stream().filter(f -> !f.isClockwise()).collect(toList());
		List<SimplePolygonXZ> innerRings = faces.stream().filter(f -> f.isClockwise()).collect(toList());
		return buildPolygonsFromRings(outerRings, innerRings);

	}

	//TODO deduplicate with MultipolygonAreaBuilder
	private static final Collection<PolygonWithHolesXZ> buildPolygonsFromRings(
			List<SimplePolygonXZ> outerRings, List<SimplePolygonXZ> innerRings) {

		Collection<PolygonWithHolesXZ> result = new ArrayList<>();

		while (!outerRings.isEmpty()) {

			/* find an outer ring */

			SimplePolygonXZ outerRing = outerRings.remove(outerRings.size() - 1);

			/* find inner rings of that ring */

			List<SimplePolygonXZ> holes = new ArrayList<>();

			for (SimplePolygonXZ innerRing : innerRings) {
				if (outerRing.getArea() > innerRing.getArea() + 1e-7 //TODO: better solution? This is because outer and inner are identical for simple closed shapes.
						&& outerRing.contains(innerRing)) {
					boolean containedInSmallerOuterRings = outerRings.stream().anyMatch(
							o -> o.contains(innerRing)
							&& o.getArea() > innerRing.getArea() + 1e-7 //TODO: better solution? This is because outer and inner are identical for simple closed shapes.
							&& outerRing.contains(o));
					if (!containedInSmallerOuterRings) {
						holes.add(innerRing);
					}
				}
			}

			/* create a new area and remove the used inner rings */

			result.add(new PolygonWithHolesXZ(outerRing, holes));
			innerRings.removeAll(holes);

		}

		return result;

	}

}
