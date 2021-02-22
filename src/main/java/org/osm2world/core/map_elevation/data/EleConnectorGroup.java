package org.osm2world.core.map_elevation.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

	//TODO make private
	public final List<EleConnector> eleConnectors;

	public EleConnectorGroup() {
		this(new ArrayList<EleConnector>());
	}

	private EleConnectorGroup(List<EleConnector> eleConnectors) {
		this.eleConnectors = eleConnectors;
	}

	public void addConnectorsFor(Iterable<VectorXZ> positions,
			Object reference, GroundState groundState) {

		for (VectorXZ pos : positions) {
			eleConnectors.add(new EleConnector(pos, reference, groundState));
		}

	}

	public void addConnectorsFor(PolygonWithHolesXZ polygon,
			Object reference, GroundState groundState) {

		addConnectorsFor(polygon.getOuter().getVertices(), reference, groundState);

		for (SimplePolygonXZ hole : polygon.getHoles()) {
			addConnectorsFor(hole.getVertices(), reference, groundState);
		}

	}

	public void addConnectorsForTriangulation(Iterable<TriangleXZ> triangles,
			Object reference, GroundState groundState) {
		//TODO check later whether this method is still necessary

		Set<VectorXZ> positions = new HashSet<VectorXZ>();

		for (TriangleXZ t : triangles) {
			positions.add(t.v1);
			positions.add(t.v2);
			positions.add(t.v3);
		}

		addConnectorsFor(positions, null, groundState);

	}

	public void add(EleConnector newConnector) {

		eleConnectors.add(newConnector);

	}

	public void addAll(Iterable<EleConnector> newConnectors) {

		for (EleConnector c : newConnectors) {
			eleConnectors.add(c);
		}

	}

	public EleConnector getConnector(VectorXZ pos) {
		//TODO review this method (parameters sufficient? necessary at all?)

		for (EleConnector eleConnector : eleConnectors) {
			if (eleConnector.pos.equals(pos)) {
				return eleConnector;
			}
		}

		return null;
		//TODO maybe ... throw new IllegalArgumentException();

	}

	public List<EleConnector> getConnectors(Iterable<VectorXZ> positions) {

		List<EleConnector> connectors = new ArrayList<EleConnector>();

		for (VectorXZ pos : positions) {
			EleConnector connector = getConnector(pos);
			connectors.add(connector);
			if (connector == null) {
				throw new IllegalArgumentException();
			}
		}

		return connectors;

	}

	public VectorXYZ getPosXYZ(VectorXZ pos) {

		EleConnector c = getConnector(pos);

		if (c != null) {

			return c.getPosXYZ();

		} else {

			return pos.xyz(0);
			//TODO maybe ... throw new IllegalArgumentException();

		}

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

	public List<TriangleXYZ> getTriangulationXYZ(List<? extends TriangleXZ> trianglesXZ) {

		List<TriangleXYZ> trianglesXYZ = new ArrayList<>(trianglesXZ.size());

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

	public static final EleConnectorGroup EMPTY = new EleConnectorGroup(
			Collections.<EleConnector>emptyList());

}
