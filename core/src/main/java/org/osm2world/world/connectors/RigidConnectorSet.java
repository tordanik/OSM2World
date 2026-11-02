package org.osm2world.world.connectors;

import static java.util.Collections.unmodifiableSet;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * a set of {@link Connector}s which can only move up or down together.
 * Each connector has a fixed vertical offset relative to the set's base elevation.
 * <p>
 * Examples include all connectors of a building (outline connectors at the base elevation,
 * entrances at the height of their level) or all connectors at one end of a bridge.
 */
public final class RigidConnectorSet implements EleRelation {

	/** how to derive the set's base elevation from the terrain elevation at those members which are on the ground */
	public enum GroundEleRule {
		/** use the lowest value, e.g. for buildings, which should not float above the terrain */
		LOWEST,
		/** use the highest value, e.g. for the ends of bridges, which should not be buried in the terrain */
		HIGHEST
	}

	private final GroundEleRule groundEleRule;
	private final Map<Connector, Double> eleOffsets;

	/**
	 * @param eleOffsets  each connector's vertical offset relative to the set's base elevation
	 */
	public RigidConnectorSet(GroundEleRule groundEleRule, Map<Connector, Double> eleOffsets) {
		if (eleOffsets.isEmpty()) throw new IllegalArgumentException("empty set");
		this.groundEleRule = groundEleRule;
		this.eleOffsets = new LinkedHashMap<>(eleOffsets);
	}

	/** creates a set where all connectors have the same elevation */
	public static RigidConnectorSet sameEle(GroundEleRule groundEleRule, Collection<Connector> connectors) {
		Map<Connector, Double> offsets = new LinkedHashMap<>();
		connectors.forEach(c -> offsets.put(c, 0.0));
		return new RigidConnectorSet(groundEleRule, offsets);
	}

	public GroundEleRule getGroundEleRule() {
		return groundEleRule;
	}

	public Set<Connector> getConnectors() {
		return unmodifiableSet(eleOffsets.keySet());
	}

	/** returns a connector's vertical offset relative to the set's base elevation */
	public double getOffset(Connector connector) {
		Double offset = eleOffsets.get(connector);
		if (offset == null) throw new IllegalArgumentException("connector is not part of this set");
		return offset;
	}

	@Override
	public String toString() {
		return "RigidConnectorSet" + eleOffsets.values();
	}

}
