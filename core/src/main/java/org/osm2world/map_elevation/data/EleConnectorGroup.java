package org.osm2world.map_elevation.data;

import java.util.*;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.*;
import org.osm2world.world.data.WorldObject;


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
		this.addConnectorsFor(positions, reference, groundState, null, null);
	}

	public void addConnectorsFor(Iterable<VectorXZ> positions, Object reference, GroundState groundState,
			@Nullable EleConnector.AttachmentProperties attachmentProperties, @Nullable WorldObject object) {

		for (VectorXZ pos : positions) {
			eleConnectors.add(new EleConnector(pos, reference, groundState, attachmentProperties, object));
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
			Object reference, GroundState groundState,
			@Nullable EleConnector.AttachmentProperties attachmentProperties, @Nullable WorldObject object) {
		//TODO check later whether this method is still necessary

		Set<VectorXZ> positions = new HashSet<>();

		for (TriangleXZ t : triangles) {
			positions.add(t.v1);
			positions.add(t.v2);
			positions.add(t.v3);
		}

		addConnectorsFor(positions, reference, groundState, attachmentProperties, object);

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
		return new PolygonXYZ(getPosXYZ(polygon.vertices()));
	}

	public PolygonWithHolesXYZ getPosXYZ(PolygonWithHolesXZ polygon) {
		return new PolygonWithHolesXYZ(
				getPosXYZ(polygon.getOuter()),
				polygon.getRings().stream().map(this::getPosXYZ).toList());
	}

	@Nonnull
	@Override
	public Iterator<EleConnector> iterator() {
		return eleConnectors.iterator();
	}

	public Stream<EleConnector> stream() {
		return eleConnectors.stream();
	}

	public static final EleConnectorGroup EMPTY = new EleConnectorGroup(
			Collections.<EleConnector>emptyList());

}
