package org.osm2world.core.map_elevation.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
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

	public void addConnectorsForTriangulation(Iterable<TriangleXZ> triangles) {
		//TODO check later whether this method is still necessary
		
		Set<VectorXZ> positions = new HashSet<VectorXZ>();
		
		for (TriangleXZ t : triangles) {
			positions.add(t.v1);
			positions.add(t.v2);
			positions.add(t.v3);
		}
		
		addConnectorsFor(positions);
		
	}
	
	public void addAll(Iterable<EleConnector> newConnectors) {
		
		for (EleConnector c : newConnectors) {
			eleConnectors.add(c);
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

	public List<VectorXYZ> getPosXYZ(Collection<VectorXZ> positions) {
		
		List<VectorXYZ> result = new ArrayList<VectorXYZ>(positions.size());
		
		for (VectorXZ pos : positions) {
			result.add(getPosXYZ(pos));
		}
		
		return result;
		
	}

	public PolygonXYZ getPosXYZ(SimplePolygonXZ polygon) {
		return new PolygonXYZ(getPosXYZ(polygon.getVertexLoop()));
	}
	
	public Collection<TriangleXYZ> getTriangulationXYZ(
			Collection<? extends TriangleXZ> trianglesXZ) {
		
		Collection<TriangleXYZ> trianglesXYZ =
				new ArrayList<TriangleXYZ>(trianglesXZ.size());
		
		for (TriangleXZ triangleXZ : trianglesXZ) {
			
			VectorXYZ v1 = getPosXYZ(triangleXZ.v1);
			VectorXYZ v2 = getPosXYZ(triangleXZ.v2);
			VectorXYZ v3 = getPosXYZ(triangleXZ.v3);
			
			if (triangleXZ.isClockwise()) { //TODO: ccw test should not be in here, but maybe in triangulation util
				trianglesXYZ.add(new TriangleXYZ(v3, v2, v1));
			} else  {
				trianglesXYZ.add(new TriangleXYZ(v1, v2, v3));
			}
			
		}
		
		return trianglesXYZ;
		
	}
	
	@Override
	public Iterator<EleConnector> iterator() {
		return eleConnectors.iterator();
	}
	
}
