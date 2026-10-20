package org.osm2world.world.connectors;

import java.util.*;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.*;
import org.osm2world.world.data.WorldObject;


/**
 * A group of {@link Connector}s.
 * This only to make it easier for {@link WorldObject}s to create and manage a large number of {@link Connector}s.
 */
public class ConnectorGroup implements Iterable<Connector> {

	//TODO make private
	public final List<Connector> connectors;

	public ConnectorGroup() {
		this(new ArrayList<>());
	}

	private ConnectorGroup(List<Connector> connectors) {
		this.connectors = connectors;
	}

	public void addConnectorsFor(Iterable<VectorXZ> positions,
			Object reference, GroundState groundState) {
		this.addConnectorsFor(positions, reference, groundState, null, null);
	}

	public void addConnectorsFor(Iterable<VectorXZ> positions, Object reference, GroundState groundState,
			@Nullable Connector.AttachmentProperties attachmentProperties, @Nullable WorldObject object) {

		for (VectorXZ pos : positions) {
			connectors.add(new Connector(pos, reference, groundState, attachmentProperties, object));
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
			@Nullable Connector.AttachmentProperties attachmentProperties, @Nullable WorldObject object) {
		//TODO check later whether this method is still necessary

		Set<VectorXZ> positions = new HashSet<>();

		for (TriangleXZ t : triangles) {
			positions.add(t.v1);
			positions.add(t.v2);
			positions.add(t.v3);
		}

		addConnectorsFor(positions, reference, groundState, attachmentProperties, object);

	}

	public void add(Connector newConnector) {

		connectors.add(newConnector);

	}

	public void addAll(Iterable<Connector> newConnectors) {

		for (Connector c : newConnectors) {
			connectors.add(c);
		}

	}

	public Connector getConnector(VectorXZ pos) {
		//TODO review this method (parameters sufficient? necessary at all?)

		for (Connector connector : connectors) {
			if (connector.pos.equals(pos)) {
				return connector;
			}
		}

		return null;
		//TODO maybe ... throw new IllegalArgumentException();

	}

	public List<Connector> getConnectors(Iterable<VectorXZ> positions) {

		List<Connector> connectors = new ArrayList<>();

		for (VectorXZ pos : positions) {
			Connector connector = getConnector(pos);
			connectors.add(connector);
			if (connector == null) {
				throw new IllegalArgumentException();
			}
		}

		return connectors;

	}

	public VectorXYZ getPosXYZ(VectorXZ pos) {

		Connector c = getConnector(pos);

		if (c != null) {

			return c.getPosXYZ();

		} else {

			return pos.xyz(0);
			//TODO maybe ... throw new IllegalArgumentException();

		}

	}

	public List<VectorXYZ> getPosXYZ(Collection<VectorXZ> positions) {

		List<VectorXYZ> result = new ArrayList<>(positions.size());

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

	@Override
	public @Nonnull Iterator<Connector> iterator() {
		return connectors.iterator();
	}

	public Stream<Connector> stream() {
		return connectors.stream();
	}

	public static final ConnectorGroup EMPTY = new ConnectorGroup(Collections.emptyList());

}
