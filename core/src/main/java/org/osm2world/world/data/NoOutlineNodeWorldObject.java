package org.osm2world.world.data;

import static java.util.Collections.singleton;

import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.math.BoundedObject;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;

/**
 * superclass for {@link NodeWorldObject}s that don't have an outline,
 * and are not part of a network.
 * Instead, they are located at a single point on the terrain or other areas
 * and not connected to other features.
 *
 * @see OutlineNodeWorldObject
 */
public abstract class NoOutlineNodeWorldObject implements NodeWorldObject, BoundedObject {

	protected final MapNode node;

	private final EleConnector connector;

	public NoOutlineNodeWorldObject(MapNode node) {
		this.node = node;
		this.connector = new EleConnector(node.getPos(), node,
				getGroundState());
	}

	@Override
	public final MapNode getPrimaryMapElement() {
		return node;
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return new AxisAlignedRectangleXZ(
				node.getPos().x, node.getPos().z,
				node.getPos().x, node.getPos().z);
	}

	@Override
	public Iterable<EleConnector> getEleConnectors() {
		return singleton(connector);
	}

	@Override
	public void defineEleConstraints(EleConstraintEnforcer enforcer) {}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + node + ")";
	}

	/**
	 * provides subclasses with the 3d position of the {@link MapNode}.
	 * Only works during rendering (i.e. after elevation calculation).
	 */
	protected VectorXYZ getBase() {
		return connector.getPosXYZ();
	}

}
