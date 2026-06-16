package org.osm2world.world.modules.building.roof;

import static java.util.Comparator.comparingDouble;
import static org.osm2world.math.algorithms.GeometryUtil.insertIntoPolygon;

import java.util.*;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.Intersection;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

/**
 * A roof that is subdivided into multiple faces by lines (ridges or edges) inside the roof polygon.
 * These lines may be calculated automatically based on a roof shape, or they may be explicitly mapped.
 * (See {@link ComplexRoof} for the latter.)
 */
public abstract class RoofWithInnerLines extends HeightfieldRoof {

	protected record InnerLine(
		LineSegmentXZ segment,
		boolean extendAtStart,
		boolean extendAtEnd
	) {

		InnerLine(LineSegmentXZ segment) {
			this(segment, false, false);
		}

		InnerLine(LineSegmentXZ segment, boolean extendAtStartAndEnd) {
			this(segment, extendAtStartAndEnd, extendAtStartAndEnd);
		}

		/**
		 * returns a line segment which may be slightly extended at the start and end to create intersections
		 * where the segment ends directly at the outline
		 */
		private LineSegmentXZ extendedSegment() {
			if (!extendAtStart && !extendAtEnd) {
				return segment;
			} else {
				return new LineSegmentXZ(
						extendAtStart ? segment.p1.add(segment.getDirection().mult(-0.05)) : segment.p1,
						extendAtEnd ? segment.p2.add(segment.getDirection().mult(0.05)) : segment.p2);
			}
		}

	}

	/**
	 * the results of #calculatePolygonAndInnerSegments()
	 *
	 * @param polygon  modified polygon for {@link #getPolygon()}
	 * @param innerSegments  inner segments for {@link #getInnerSegments()}
	 * @param extraPoints  extra points that were created at intersections,
	 *                     mapped to their originating {@link InnerLine}s.
	 *                     Some subclasses may use this for their height calculations.
	 */
	protected record CalculationResults(
			PolygonWithHolesXZ polygon,
			Collection<LineSegmentXZ> innerSegments,
			Map<VectorXZ, InnerLine> extraPoints

	) {}

	public RoofWithInnerLines(@Nullable BuildingPart buildingPart, PolygonWithHolesXZ originalPolygon,
			TagSet tags, Material material) {
		super(buildingPart, originalPolygon, tags, material);
	}

	@Override
	public final Collection<LineSegmentXZ> getInnerSegments() {
		return calculatePolygonAndInnerSegments().innerSegments();
	}

	@Override
	public final PolygonWithHolesXZ getPolygon() {
		return calculatePolygonAndInnerSegments().polygon();
	}

	/**
	 * Method for the subclasses to supply the inner lines which structure this roof shape.
	 * These are the "raw" lines which may still exceed the outline etc., they will be processed by this class
	 * before the result is provided through {@link #getInnerSegments()}.
	 */
	protected abstract Collection<InnerLine> getInnerLines();

	protected CalculationResults calculatePolygonAndInnerSegments() {

		SimplePolygonXZ newOuter = originalPolygon.getOuter();
		List<SimplePolygonXZ> newHoles = originalPolygon.getHoles().isEmpty()
				? List.of() : new ArrayList<>(originalPolygon.getHoles());

		List<LineSegmentXZ> innerSegments = new ArrayList<>();
		Map<VectorXZ, InnerLine> extraPoints = new HashMap<>();

		for (InnerLine innerLine : getInnerLines()) {

			LineSegmentXZ segment = innerLine.extendedSegment();

			List<VectorXZ> splitPoints = new ArrayList<>();

			/* insert new points at all intersections between the outline and the ridge or other inner segments */

			for (Intersection intersection : newOuter.intersections(segment)) {
				newOuter = insertIntoPolygon(newOuter, intersection.point(), SNAP_DISTANCE);
				splitPoints.add(intersection.point());
				extraPoints.put(intersection.point(), innerLine);
			}

			for (int i = 0; i < newHoles.size(); i++) {
				SimplePolygonXZ hole = newHoles.get(i);
				for (Intersection intersection : hole.intersections(segment)) {
					hole = insertIntoPolygon(hole, intersection.point(), SNAP_DISTANCE);
					newHoles.set(i, hole);
					splitPoints.add(intersection.point());
					extraPoints.put(intersection.point(), innerLine);
				}
			}

			/* complete the split points with the start and end points of the segments */

			for (VectorXZ p : List.of(segment.p1, segment.p2)) {
				// prefer points previously inserted into the outline to ensure exact match during face decomposition
				if (splitPoints.stream().noneMatch(sp -> sp.distanceTo(p) < SNAP_DISTANCE)) {
					splitPoints.add(p);
				}
			}

			/* split the line at intersection points */

			splitPoints.sort(comparingDouble(p -> p.distanceTo(segment.p1)));

			for (int i = 0; i + 1 < splitPoints.size(); i++) {
				LineSegmentXZ s = new LineSegmentXZ(splitPoints.get(i), splitPoints.get(i + 1));
				if (originalPolygon.contains(s.getCenter())) {
					innerSegments.add(s);
				}
			}

		}

		return new CalculationResults(
				new PolygonWithHolesXZ(newOuter, newHoles),
				innerSegments, extraPoints);

	}

}
