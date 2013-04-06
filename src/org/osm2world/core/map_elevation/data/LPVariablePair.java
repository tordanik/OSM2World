package org.osm2world.core.map_elevation.data;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.VectorXYZ;

/**
 * a pair of variables for a linear program,
 * representing the elevation of a set of {@link EleConnector}s.
 */
public class LPVariablePair {
	
	private final List<EleConnector> connectors = new ArrayList<EleConnector>(2);
	
	/**
	 * creates a variable pair for a first {@link EleConnector}.
	 * Others may be added later.
	 */
	public LPVariablePair(EleConnector firstMember) {
		connectors.add(firstMember);
	}
	
	/**
	 * returns all connectors currently in this set.
	 * Must return the same object over the lifetime of this
	 * {@link LPVariablePair} instance.
	 */
	public List<EleConnector> getConnectors() {
		return connectors;
	}
	
	/**
	 * adds a connector to the set of {@link EleConnector}s whose elevation
	 * is represented by this {@link LPVariablePair}
	 */
	public void add(EleConnector c) {
		connectors.add(c);
	}
	
	/**
	 * adds all connectors from another instance to this one
	 */
	public void addAll(LPVariablePair other) {
		connectors.addAll(other.getConnectors());
	}
	
	/**
	 * {@link EleConnector#setPosXYZ(VectorXYZ)} for all connectors in this set
	 */
	public void setPosXYZ(VectorXYZ posXYZ) {
		for (EleConnector c : connectors) {
			c.setPosXYZ(posXYZ);
		}
	}
	
	/**
	 * {@link EleConnector#getPosXYZ()} for this set
	 */
	public VectorXYZ getPosXYZ() {
		return connectors.get(0).getPosXYZ();
	}
	
	/**
	 * {@link EleConnector#connectsTo(EleConnector)} for this set
	 */
	public boolean connectsTo(EleConnector other) {
		return connectors.get(0).connectsTo(other);
	}
	
	/**
	 * TODO document
	 * @see #negVar()
	 */
	public Object posVar() {
		return this;
	}
	
	/**
	 * TODO document
	 * @see #posVar()
	 */
	public Object negVar() {
		return this.getConnectors();
	}
	
}
