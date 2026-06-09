package org.osm2world.world.modules.building.roof;

import static java.util.Comparator.comparingDouble;
import static org.osm2world.math.algorithms.GeometryUtil.insertIntoPolygon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
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
		Double height,
		boolean extendAtStart,
		boolean extendAtEnd
	) {

		InnerLine(LineSegmentXZ segment) {
			this(segment, null, false, false);
		}

		InnerLine(LineSegmentXZ segment, boolean extendAtStartAndEnd) {
			this(segment, null, extendAtStartAndEnd, extendAtStartAndEnd);
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

	public RoofWithInnerLines(@Nullable BuildingPart buildingPart, PolygonWithHolesXZ originalPolygon,
			TagSet tags, Material material) {
		super(buildingPart, originalPolygon, tags, material);
	}

	@Override
	public final Collection<LineSegmentXZ> getInnerSegments() {
		return calculatePolygonAndInnerSegments().getRight();
	}

	@Override
	public final PolygonWithHolesXZ getPolygon() {
		return calculatePolygonAndInnerSegments().getLeft();
	}

	protected abstract Collection<InnerLine> getInnerLines();

	private Pair<PolygonWithHolesXZ, Collection<LineSegmentXZ>> calculatePolygonAndInnerSegments() {

		SimplePolygonXZ newOuter = originalPolygon.getOuter();
		List<SimplePolygonXZ> newHoles = originalPolygon.getHoles().isEmpty()
				? List.of() : new ArrayList<>(originalPolygon.getHoles());

		List<LineSegmentXZ> innerSegments = new ArrayList<>();

		for (InnerLine innerLine : getInnerLines()) {

			LineSegmentXZ segment = innerLine.extendedSegment();

			List<VectorXZ> splitPoints = new ArrayList<>();

			/* insert new points at all intersections between the outline and the ridge or other inner segments */

			for (Intersection intersection : newOuter.intersections(segment)) {
				newOuter = insertIntoPolygon(newOuter, intersection.point(), SNAP_DISTANCE);
				splitPoints.add(intersection.point());
			}

			for (int i = 0; i < newHoles.size(); i++) {
				SimplePolygonXZ hole = newHoles.get(i);
				for (Intersection intersection : hole.intersections(segment)) {
					hole = insertIntoPolygon(hole, intersection.point(), SNAP_DISTANCE);
					newHoles.set(i, hole);
					splitPoints.add(intersection.point());
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

		return Pair.of(new PolygonWithHolesXZ(newOuter, newHoles), innerSegments);

	}

}
