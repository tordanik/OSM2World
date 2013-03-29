package org.osm2world.core.world.data;

import static java.util.Collections.singleton;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.datastructures.IntersectionTestObject;

/**
 * superclass for {@link NodeWorldObject}s that don't have an outline,
 * and are not part of a network.
 * Instead, they are located at a single point on the terrain or other areas
 * and not connected to other features.
 * 
 * @see OutlineNodeWorldObject
 */
public abstract class NoOutlineNodeWorldObject implements NodeWorldObject,
		IntersectionTestObject {
	
	protected final MapNode node;
	
	private final EleConnector connector;
	
	public NoOutlineNodeWorldObject(MapNode node) {
		this.node = node;
		this.connector = new EleConnector(node.getPos());
	}
	
	@Override
	public final MapNode getPrimaryMapElement() {
		return node;
	}
	
	@Override
	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
		return new AxisAlignedBoundingBoxXZ(singleton(node.getPos()));
	}
	
	@Override
	public Iterable<EleConnector> getEleConnectors() {
		return singleton(new EleConnector(node.getPos()));
	}
	
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
