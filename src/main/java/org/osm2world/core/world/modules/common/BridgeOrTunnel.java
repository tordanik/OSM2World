package org.osm2world.core.world.modules.common;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.osm2world.core.map_elevation.creation.EleConstraintEnforcer.ConstraintType.*;
import static org.osm2world.core.map_elevation.data.GroundState.*;
import static org.osm2world.core.math.GeometryUtil.isBetween;

import java.util.List;

import org.osm2world.core.map_data.data.MapAreaSegment;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.overlaps.MapIntersectionWW;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_data.data.overlaps.MapOverlapType;
import org.osm2world.core.map_data.data.overlaps.MapOverlapWA;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.modules.TreeModule.Forest;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;

/**
 * common superclass for bridges and tunnels
 */
public abstract class BridgeOrTunnel implements WaySegmentWorldObject {

	protected final MapWaySegment segment;
	protected final AbstractNetworkWaySegmentWorldObject primaryRep;

	public BridgeOrTunnel(MapWaySegment segment,
			AbstractNetworkWaySegmentWorldObject primaryRepresentation) {
		this.segment = segment;
		this.primaryRep = primaryRepresentation;
	}

	@Override
	public MapWaySegment getPrimaryMapElement() {
		return segment;
	}

	@Override
	public VectorXZ getEndPosition() {
		return primaryRep.getEndPosition();
	}

	@Override
	public VectorXZ getStartPosition() {
		return primaryRep.getStartPosition();
	}

	@Override
	public Iterable<EleConnector> getEleConnectors() {
		return emptyList();
	}

	@Override
	public void defineEleConstraints(EleConstraintEnforcer enforcer) {

		@SuppressWarnings("unchecked")
		List<List<VectorXZ>> lines = asList(
				primaryRep.getCenterlineXZ(),
				primaryRep.getOutlineXZ(true),
				primaryRep.getOutlineXZ(false));

		SimplePolygonXZ outlinePolygonXZ = primaryRep.getOutlinePolygonXZ();

		/* ensure a minimum vertical distance to ways and areas below,
		 * at intersections */

		for (MapOverlap<?,?> overlap : segment.getOverlaps()) {

			MapElement other = overlap.getOther(segment);
			WorldObject otherWO = other.getPrimaryRepresentation();

			if (otherWO == null
					|| otherWO.getGroundState() != ON)  //TODO remove the ground state check
				continue;

			boolean thisIsUpper = this.getGroundState() == ABOVE; //TODO check layers

			double distance = 10.0; //TODO base on clearing

			if (overlap instanceof MapIntersectionWW) {

				MapIntersectionWW intersection = (MapIntersectionWW) overlap;

				if (otherWO instanceof AbstractNetworkWaySegmentWorldObject) {

					AbstractNetworkWaySegmentWorldObject otherANWSWO =
							((AbstractNetworkWaySegmentWorldObject)otherWO);

					EleConnector thisConn = primaryRep.getEleConnectors()
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

			} else if (overlap instanceof MapOverlapWA) {

				/*
				 * require minimum distance at intersection points
				 * (these have been inserted into this segment,
				 * but not into the area)
				 */

				MapOverlapWA overlapWA = (MapOverlapWA) overlap;

				if (overlap.type == MapOverlapType.INTERSECT
						&& otherWO instanceof AbstractAreaWorldObject) {

					AbstractAreaWorldObject otherAAWO =
							((AbstractAreaWorldObject)otherWO);

					for (int i = 0; i < overlapWA.getIntersectionPositions().size(); i++) {

						VectorXZ pos =
								overlapWA.getIntersectionPositions().get(i);
						MapAreaSegment areaSegment =
								overlapWA.getIntersectingAreaSegments().get(i);

						EleConnector thisConn = primaryRep.getEleConnectors()
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

								EleConnector base1 = primaryRep.getEleConnectors().getConnector(v1);
								EleConnector base2 = primaryRep.getEleConnectors().getConnector(v2);

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

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + segment + ")";
	}

}
