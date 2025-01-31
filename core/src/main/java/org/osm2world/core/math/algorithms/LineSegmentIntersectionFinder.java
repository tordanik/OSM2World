package org.osm2world.core.math.algorithms;

import static java.lang.Double.NaN;
import static org.osm2world.core.math.algorithms.GeometryUtil.getTrueLineSegmentIntersection;

import java.util.*;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.LineSegmentXZ;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Takes a set of line segments, and detects all intersections between them.
 * Uses a sweepline algorithm that sweeps in positive x direction (left to right).
 */
public final class LineSegmentIntersectionFinder {

	/** prevents instantiation */
	private LineSegmentIntersectionFinder() {}

	/** an intersection detected by the finder */
	public record Intersection<E>(VectorXZ pos, E segmentA, E segmentB) {
		@Override
		public String toString() {
			return pos.toString();
		}
	}

	/**
	 * finds all intersections in a set of line segments.
	 * Only reports true intersections, not shared start or end points.
	 *
	 * Limitations: Does not work if three or more segments intersect in the same location.
	 */
	public static final List<Intersection<LineSegmentXZ>> findAllIntersections(Iterable<? extends LineSegmentXZ> segments) {
		return findAllIntersections(segments, Function.identity());
	}

	/**
	 * a more flexible alternative to {@link #findAllIntersections(Iterable)}
	 * @param <S>  the segment type, does not need to be {@link LineSegmentXZ}
	 * @param toLineSegmentXZ  how to get from S to {@link LineSegmentXZ}
	 */
	public static final <S> List<Intersection<S>> findAllIntersections(Iterable<? extends S> segments,
			Function<S, LineSegmentXZ> toLineSegmentXZ) {

		Multimap<OrderedSegment, S> originalSegmentsMap = HashMultimap.create();
		Set<OrderedSegment> orderedSegments = new HashSet<>();

		for (S s : segments) {
			OrderedSegment orderedSegment = OrderedSegment.from(toLineSegmentXZ.apply(s));
			originalSegmentsMap.put(orderedSegment, s);
			orderedSegments.add(orderedSegment);
		}

		List<Intersection<OrderedSegment>> rawResult = findAllIntersections_impl(orderedSegments);

		List<Intersection<S>> result = new ArrayList<>(rawResult.size());

		for (Intersection<OrderedSegment> intersection : rawResult) {
			for (S segmentA : originalSegmentsMap.get(intersection.segmentA)) {
				for (S segmentB : originalSegmentsMap.get(intersection.segmentB)) {
					result.add(new Intersection<>(intersection.pos, segmentA, segmentB));
				}
			}
		}

		return result;

	}

	private static final List<Intersection<OrderedSegment>> findAllIntersections_impl(Set<OrderedSegment> segments) {

		List<Intersection<OrderedSegment>> result = new ArrayList<>();

		/* prepare the events for where a segment begins or ends */

		List<Event> beginEndEvents = new ArrayList<>();

		segments.forEach(s -> {
			beginEndEvents.add(new BeginSegmentEvent(s));
			beginEndEvents.add(new EndSegmentEvent(s));
		});

		//beginEndEvents.sort(null);
		//TODO use two separate queues, a static and a dynamic one. Wrap that into a SweeplinePriorityQueue.

		/* set up the event queue */

		PriorityQueue<Event> eventQueue = new PriorityQueue<>(beginEndEvents);

		/* run the sweep */

		SweepLine sweepLine = new SweepLine();

		while (!eventQueue.isEmpty()) {

			Event event = eventQueue.poll();

			sweepLine.moveTo(event.getXPosition());

			if (event instanceof BeginSegmentEvent) {

				OrderedSegment segment = ((BeginSegmentEvent) event).segment;

				OrderedSegment higher = sweepLine.higher(segment);
				insertIntersectionIfAny(eventQueue, sweepLine.getCurrentX(), segment, higher);

				OrderedSegment lower = sweepLine.lower(segment);
				insertIntersectionIfAny(eventQueue, sweepLine.getCurrentX(), segment, lower);

				sweepLine.add(segment);

			} else if (event instanceof EndSegmentEvent) {

				OrderedSegment segment = ((EndSegmentEvent) event).segment;

				sweepLine.remove(segment);

				OrderedSegment upperNeighbor = sweepLine.higher(segment);
				OrderedSegment lowerNeighbor = sweepLine.lower(segment);
				insertIntersectionIfAny(eventQueue, sweepLine.getCurrentX(), upperNeighbor, lowerNeighbor);

			} else if (event instanceof IntersectionEvent) {

				Intersection<OrderedSegment> intersection = ((IntersectionEvent) event).intersection;

				/* ignore intersections that are already handled. Doing this means that duplicated entries
				 * in the event queue are ok, which saves us from ever having to remove existing events. */
				if (result.contains(intersection)) continue;

				result.add(intersection);

				/* swap the positions of the intersecting segments (by removing them, then adding them again
				 * now that the sweepline's comparator is using the new position). */
				sweepLine.remove(intersection.segmentA);
				sweepLine.remove(intersection.segmentB);
				sweepLine.add(intersection.segmentA);
				sweepLine.add(intersection.segmentB);

				OrderedSegment upperSegmentAfterIntersection, lowerSegmentAfterIntersection;

				if (sweepLine.comparator.compare(intersection.segmentA, intersection.segmentB) < 0) {
					lowerSegmentAfterIntersection = intersection.segmentA;
					upperSegmentAfterIntersection = intersection.segmentB;
				} else {
					lowerSegmentAfterIntersection = intersection.segmentB;
					upperSegmentAfterIntersection = intersection.segmentA;
				}

				OrderedSegment higher = sweepLine.higher(upperSegmentAfterIntersection);
				insertIntersectionIfAny(eventQueue, sweepLine.getCurrentX(), upperSegmentAfterIntersection, higher);

				OrderedSegment lower = sweepLine.lower(lowerSegmentAfterIntersection);
				insertIntersectionIfAny(eventQueue, sweepLine.getCurrentX(), lowerSegmentAfterIntersection, lower);

			}

		}

		return result;

	}

	private static final void insertIntersectionIfAny(PriorityQueue<Event> eventQueue, double currentSweeplineX,
			@Nullable OrderedSegment segmentA, @Nullable OrderedSegment segmentB) {

		if (segmentA == null || segmentB == null) return;

		VectorXZ pos = getTrueLineSegmentIntersection(segmentA.p1, segmentA.p2, segmentB.p1, segmentB.p2);

		if (pos != null
				&& pos.x >= currentSweeplineX) { //avoid re-inserting intersections that are "old news"
			eventQueue.add(new IntersectionEvent(new Intersection<>(pos, segmentA, segmentB)));
		}

	}

	private static final Comparator<VectorXZ> POINT_COMPARATOR =
			Comparator.comparing(VectorXZ::getX).thenComparing(VectorXZ::getZ);
	private static final Comparator<Event> EVENT_COMPARATOR =
			Comparator.comparing(Event::getXPosition).thenComparing(Event::getZPosition);

	/** a segment that has its points ordered according to {@link #POINT_COMPARATOR} */
	private static final class OrderedSegment extends LineSegmentXZ {

		public OrderedSegment(VectorXZ p1, VectorXZ p2) {
			super(p1, p2);
			if (POINT_COMPARATOR.compare(p1, p2) > 0) {
				throw new IllegalArgumentException("wrong point order");
			}
		}

		public static OrderedSegment from(LineSegmentXZ s) {
			if (POINT_COMPARATOR.compare(s.p1, s.p2) < 0) {
				return new OrderedSegment(s.p1, s.p2);
			} else {
				return new OrderedSegment(s.p2, s.p1);
			}
		}

	}

	private static interface Event extends Comparable<Event> {

		public double getXPosition();
		public double getZPosition();

		@Override
		default int compareTo(Event other) {
			return EVENT_COMPARATOR.compare(this, other);
		}

	}

	private static class BeginSegmentEvent implements Event {

		public final OrderedSegment segment;
		private final double x;
		private final double z;

		public BeginSegmentEvent(OrderedSegment segment) {
			this.segment = segment;
			VectorXZ pos = segment.p1;
			this.x = pos.x;
			this.z = pos.z;
		}

		@Override
		public double getXPosition() {
			return x;
		}

		@Override
		public double getZPosition() {
			return z;
		}

		@Override
		public String toString() {
			return "Begin {x = " + x + ", segment = " + segment + "}";
		}

	}

	private static class EndSegmentEvent implements Event {

		public final OrderedSegment segment;
		private final double x;
		private final double z;

		public EndSegmentEvent(OrderedSegment segment) {
			this.segment = segment;
			VectorXZ pos = segment.p2;
			this.x = pos.x;
			this.z = pos.z;
		}

		@Override
		public double getXPosition() {
			return x;
		}

		@Override
		public double getZPosition() {
			return z;
		}

		@Override
		public String toString() {
			return "End {x = " + x + ", segment = " + segment + "}";
		}

	}

	private record IntersectionEvent(
			Intersection<OrderedSegment> intersection) implements Event {

		@Override
		public double getXPosition() {
			return intersection.pos.x;
		}

		@Override
		public double getZPosition() {
			return intersection.pos.z;
		}

		@Override
		public String toString() {
			return "Intersection " + intersection.pos;
		}

	}

	/**
	 * a data structure that contains and orders all segments currently under the sweepline.
	 * Wraps a {@link TreeSet} with a custom comparator.
	 */
	private static class SweepLine {

		private double currentX = NaN;

		public void moveTo(double currentX) {
			if (currentX < this.currentX) throw new IllegalArgumentException("moving backward is illegal");
			this.currentX = currentX;
		}

		public double getCurrentX() {
			return currentX;
		}

		/**
		 * compares line segments based on where the sweepline currently is.
		 * (That makes a big difference: After an intersection, the order of the intersecting segments will be swapped.)
		 *
		 * It's ok for the comparator to be mutable: only changes at intersections, and there will be an event for each.
		 */
		private final Comparator<OrderedSegment> comparator = (OrderedSegment s1, OrderedSegment s2) -> {
			if (Double.isNaN(currentX)) throw new IllegalStateException("sweeplineX unset");
			double x = currentX + 1e-5; //small offset ensures a sensible order if we're exactly at an intersection
			VectorXZ p1 = new VectorXZ(x, s1.evaluateAtX(x));
			VectorXZ p2 = new VectorXZ(x, s2.evaluateAtX(x));
			return POINT_COMPARATOR.compare(p1, p2);
		};

		private final TreeSet<OrderedSegment> tree = new TreeSet<>(this.comparator);


		public void add(OrderedSegment segment) {
			tree.add(segment);
		}

		public void remove(OrderedSegment segment) {
			tree.remove(segment);
		}

		public OrderedSegment lower(OrderedSegment segment) {
			return tree.lower(segment);
		}

		public OrderedSegment higher(OrderedSegment segment) {
			return tree.higher(segment);
		}

	}

}