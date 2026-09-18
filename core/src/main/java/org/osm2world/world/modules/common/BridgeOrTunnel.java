package org.osm2world.world.modules.common;

import static java.util.Collections.emptyList;
import static org.osm2world.map_elevation.creation.EleConstraintEnforcer.ConstraintType.MAX;
import static org.osm2world.map_elevation.creation.EleConstraintEnforcer.ConstraintType.MIN;
import static org.osm2world.map_elevation.data.GroundState.ABOVE;
import static org.osm2world.map_elevation.data.GroundState.ON;
import static org.osm2world.math.algorithms.GeometryUtil.isBetween;

import java.util.List;

import org.osm2world.map_data.data.MapAreaSegment;
import org.osm2world.map_data.data.MapElement;
import org.osm2world.map_data.data.overlaps.MapIntersectionWW;
import org.osm2world.map_data.data.overlaps.MapOverlap;
import org.osm2world.map_data.data.overlaps.MapOverlapType;
import org.osm2world.map_data.data.overlaps.MapOverlapWA;
import org.osm2world.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.world.data.AbstractAreaWorldObject;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.data.WaySegmentWorldObject;
import org.osm2world.world.data.WorldObject;
import org.osm2world.world.modules.TreeModule.Forest;
import org.osm2world.world.network.AbstractNetworkWaySegmentWorldObject;

/**
 * common superclass for bridges and tunnels
 */
public interface BridgeOrTunnel extends WaySegmentWorldObject, ProceduralWorldObject {

	AbstractNetworkWaySegmentWorldObject getPrimaryRep();

	@Override
	default VectorXZ getEndPosition() {
		return getPrimaryRep().getEndPosition();
	}

	@Override
	default VectorXZ getStartPosition() {
		return getPrimaryRep().getStartPosition();
	}

	@Override
	default Iterable<EleConnector> getEleConnectors() {
		return emptyList();
	}

	@Override
	default void defineEleConstraints(EleConstraintEnforcer enforcer) {

		List<List<VectorXZ>> lines = List.of(
				getPrimaryRep().getCenterlineXZ().vertices(),
				getPrimaryRep().getOutlineXZ(true),
				getPrimaryRep().getOutlineXZ(false));

		SimplePolygonXZ outlinePolygonXZ = getPrimaryRep().getOutlinePolygonXZ();

		/* ensure a minimum vertical distance to ways and areas below,
		 * at intersections */

		for (MapOverlap<?,?> overlap : getPrimaryRep().segment.getOverlaps()) {

			MapElement other = overlap.getOther(getPrimaryRep().segment);
			WorldObject otherWO = other.getPrimaryRepresentation();

			if (otherWO == null
					|| otherWO.getGroundState() != ON)  //TODO remove the ground state check
				continue;

			boolean thisIsUpper = this.getGroundState() == ABOVE; //TODO check layers

			double distance = 10.0; //TODO base on clearing

			if (overlap instanceof MapIntersectionWW intersection) {

				if (otherWO instanceof AbstractNetworkWaySegmentWorldObject otherANWSWO) {

					EleConnector thisConn = getPrimaryRep().getEleConnectors()
							.getConnector(intersection.pos);
					EleConnector otherConn = otherANWSWO.getEleConnectors()
							.getConnector(intersection.pos);

					if (thisIsUpper) {
						enforcer.requireVerticalDistance(
								MIN, distance, thisConn, otherConn);
					} else {
						enforcer.requireVerticalDistance(
								MIN, distance, otherConn, thisConn);
					}

				}

			} else if (overlap instanceof MapOverlapWA overlapWA) {

				/*
				 * require minimum distance at intersection points
				 * (these have been inserted into this segment,
				 * but not into the area)
				 */

				if (overlap.type == MapOverlapType.INTERSECT
						&& otherWO instanceof AbstractAreaWorldObject otherAAWO) {

					for (int i = 0; i < overlapWA.getIntersectionPositions().size(); i++) {

						VectorXZ pos =
								overlapWA.getIntersectionPositions().get(i);
						MapAreaSegment areaSegment =
								overlapWA.getIntersectingAreaSegments().get(i);

						EleConnector thisConn = getPrimaryRep().getEleConnectors()
								.getConnector(pos);

						EleConnector base1 = otherAAWO.getEleConnectors()
								.getConnector(areaSegment.getStartNode().getPos());
						EleConnector base2 = otherAAWO.getEleConnectors()
								.getConnector(areaSegment.getEndNode().getPos());

						if (thisConn != null && base1 != null && base2 != null) {

							if (thisIsUpper) {
								enforcer.requireVerticalDistance(MIN, distance,
										thisConn, base1, base2);
							} else {
								enforcer.requireVerticalDistance(MAX, -distance,
										thisConn, base1, base2);
							}

						}

					}

				}

				/*
				 * require minimum distance to the area's elevation connectors.
				 * There is usually no direct counterpart for these in this segment.
				 * Examples include trees on terrain above tunnels.
				 */

				if (!(otherWO instanceof Forest)) continue; //TODO enable and debug for other WO classes

				eleConnectors:
				for (EleConnector c : otherWO.getEleConnectors()) {

					if (outlinePolygonXZ == null ||
							!outlinePolygonXZ.contains(c.pos))
						continue eleConnectors;

					for (List<VectorXZ> line : lines) {
						for (int i = 0; i+1 < line.size(); i++) {

							VectorXZ v1 = line.get(i);
							VectorXZ v2 = line.get(i+1);

							if (isBetween(c.pos, v1, v2)) {

								EleConnector base1 = getPrimaryRep().getEleConnectors().getConnector(v1);
								EleConnector base2 = getPrimaryRep().getEleConnectors().getConnector(v2);

								if (base1 != null && base2 != null) {

									if (thisIsUpper) {
										enforcer.requireVerticalDistance(
												MAX, -distance,
												c, base1, base2);
									} else {
										enforcer.requireVerticalDistance(
												MIN, distance,
												c, base1, base2);
									}

								}

								continue eleConnectors;

							}

						}
					}

				}

			}

		}
	}

}
