package org.osm2world.core.map_elevation.data;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.VectorXYZ;

/**
 * a set of {@link EleConnector}s which are joined.
 */
public class JoinedEleConnectors {
	
	/** all members of the set. */
	private final List<EleConnector> connectors = new ArrayList<EleConnector>(2);
	
	public JoinedEleConnectors(EleConnector firstMember) {
		connectors.add(firstMember);
	}
	
	/**
	 * returns all connectors currently in this set.
	 * This must return the same object over the lifetime of this
	 * {@link JoinedEleConnectors} instance.
	 */
	public List<EleConnector> getConnectors() {
		return connectors;
	}
	
	public void add(EleConnector c) {
		assert this.connectsTo(c) && this.getPosXYZ() == c.getPosXYZ();
		connectors.add(c);
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
	
}
