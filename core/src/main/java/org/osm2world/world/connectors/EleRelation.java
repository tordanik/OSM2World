package org.osm2world.world.connectors;

import org.osm2world.map_elevation.creation.EleCalculator;
import org.osm2world.world.data.WorldObject;

/**
 * a rule about how the elevations of several {@link Connector}s relate to each other.
 * Relations are provided by {@link WorldObject#getEleRelations()} and enforced by an {@link EleCalculator}.
 * <p>
 * Connectors which {@link Connector#connectsTo(Connector)} each other are always kept at the same elevation,
 * this does not need to be expressed as a relation.
 */
public sealed interface EleRelation permits RigidConnectorSet, LowestGroundContact {}
