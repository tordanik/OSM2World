package org.osm2world.core.map_data.data.overlaps;

import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapAreaSegment;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.VectorXZ;

/**
 * overlap between a {@link MapWaySegment} and a {@link MapArea} ("Way-Area").
 * The way either intersects with the area
 * or is completely contained within the area.
 */
public class MapOverlapWA extends MapOverlap<MapWaySegment, MapArea> {

	//TODO maybe create a MapIntersection object for each of these?
	private final List<VectorXZ> intersectionPositions;
	private final List<MapAreaSegment> intersectingAreaSegments;

	public MapOverlapWA(MapWaySegment waySegment, MapArea area,
			MapOverlapType type, List<VectorXZ> intersectionPositions,
			List<MapAreaSegment> intersectingAreaSegments) {

		super(waySegment, area, type);

		this.intersectionPositions = intersectionPositions;
		this.intersectingAreaSegments = intersectingAreaSegments;

	}

	public MapArea getOther(MapWaySegment waySegment) {
		return (MapArea) super.getOther(waySegment);
	}

	public MapWaySegment getOther(MapArea area) {
		return (MapWaySegment) super.getOther(area);
	}

	/** same order as {@link #getIntersectingAreaSegments()} */
	public List<VectorXZ> getIntersectionPositions() {
		return intersectionPositions;
	}

	/** same order as {@link #getIntersectionPositions()} */
	public List<MapAreaSegment> getIntersectingAreaSegments() {
		return intersectingAreaSegments;
	}

	public Collection<LineSegmentXZ> getOverlappedSegments() {

		if (type == MapOverlapType.CONTAIN) {

			return singletonList(e1.getLineSegment());

		} else if (type == MapOverlapType.INTERSECT) {

			/* order intersections and start/end nodes
			 * by distance from the way segments' start node */

			List<VectorXZ> positions = new ArrayList<VectorXZ>(getIntersectionPositions());
			positions.add(e1.getStartNode().getPos());
			positions.add(e1.getEndNode().getPos());

			sort(positions, new Comparator<VectorXZ>() {
				public int compare(VectorXZ v1, VectorXZ v2) {
					return Double.compare(
							VectorXZ.distance(v1, e1.getStartNode().getPos()),
							VectorXZ.distance(v2, e1.getStartNode().getPos()));
				}
			});

			/* check for each line segments between two positions
			 * whether it overlaps with the area */

			List<LineSegmentXZ> result = new ArrayList<LineSegmentXZ>();

			for (int i = 0; i+1 < positions.size(); i++) {

				LineSegmentXZ segment =
					new LineSegmentXZ(positions.get(i), positions.get(i+1));

				if (e2.getPolygon().contains(segment.getCenter())) {
					result.add(segment);
				}

			}

			return result;

		} else {

			return emptyList();

		}
	}

	public Collection<LineSegmentXZ> getSharedSegments() {

		if (type == MapOverlapType.SHARE_SEGMENT) {

			for (MapAreaSegment areaSegment : e2.getAreaSegments()) {
				if (areaSegment.sharesBothNodes(e1)) {
					return singletonList(areaSegment.getLineSegment());
				}
			}

			throw new Error("no shared segment found");

		} else {

			return emptyList();

		}

	}

}
