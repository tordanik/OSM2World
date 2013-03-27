package org.osm2world.core.map_elevation.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;


/**
 * TODO document
 * 
 * This only exists to make it easier for WorldObjects to manage a large
 * number of EleConnectors.
 */
public class EleConnectorGroup implements Iterable<EleConnector> {

	private final List<EleConnector> eleConnectors = new ArrayList<EleConnector>();
	
	public void addConnectorsFor(Iterable<VectorXZ> positions) {
		for (VectorXZ pos : positions) {
			eleConnectors.add(new EleConnector(pos));
		}
	}
	
	public void addConnectorsFor(PolygonWithHolesXZ polygon) {
		addConnectorsFor(polygon.getOuter().getVertices());
		for (SimplePolygonXZ hole : polygon.getHoles()) {
			addConnectorsFor(hole.getVertices());
		}
	}
	
	public VectorXYZ getPosXYZ(VectorXZ pos) {
		
		for (EleConnector eleConnector : eleConnectors) {
			if (eleConnector.pos.equals(pos)) {
				return eleConnector.getPosXYZ();
			}
		}
		
		return pos.xyz(0);
		//TODO maybe ... throw new IllegalArgumentException();
		
	}
	
	public PolygonXYZ getPosXYZ(SimplePolygonXZ polygon) {
		
		List<VectorXYZ> vertexLoop = new ArrayList<VectorXYZ>(polygon.size() + 1);
		
		for (VectorXZ pos : polygon.getVertexLoop()) {
			vertexLoop.add(getPosXYZ(pos));
		}
		
		return new PolygonXYZ(vertexLoop);
		
	}
	
	@Override
	public Iterator<EleConnector> iterator() {
		return eleConnectors.iterator();
	}
	
}
