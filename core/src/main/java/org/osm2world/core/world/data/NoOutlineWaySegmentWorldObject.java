package org.osm2world.core.world.data;

import static java.util.Arrays.asList;

import java.util.List;

import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.BoundedObject;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

/**
 * superclass for {@link WaySegmentWorldObject}s that don't have an outline,
 * and are not part of a network.
 * Instead, they can be considered infinitely thin.
 */
public abstract class NoOutlineWaySegmentWorldObject implements WaySegmentWorldObject, BoundedObject {

	protected final MapWaySegment segment;

	private final EleConnector startConnector;
	private final EleConnector endConnector;

	public NoOutlineWaySegmentWorldObject(MapWaySegment segment) {

		this.segment = segment;

		startConnector = new EleConnector(getStartPosition(),
				segment.getStartNode(), getGroundState());
		endConnector = new EleConnector(getEndPosition(),
				segment.getEndNode(), getGroundState());

	}

	@Override
	public final MapWaySegment getPrimaryMapElement() {
		return segment;
	}

	@Override
	public Iterable<EleConnector> getEleConnectors() {
		return asList(startConnector, endConnector);
	}

	@Override
	public void defineEleConstraints(EleConstraintEnforcer enforcer) {}

	@Override
	public VectorXZ getStartPosition() {
		return segment.getStartNode().getPos();
	}

	@Override
	public VectorXZ getEndPosition() {
		return segment.getEndNode().getPos();
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return segment.boundingBox();
	}

	/**
	 * returns the 3d start position.
	 * Only available after elevation calculation.
	 */
	protected VectorXYZ getStartXYZ() {
		return startConnector.getPosXYZ();
	}

	/**
	 * returns the 3d end position.
	 * Only available after elevation calculation.
	 */
	protected VectorXYZ getEndXYZ() {
		return endConnector.getPosXYZ();
	}

	/**
	 * returns the 3d vertex sequence running along the segment.
	 * Only available after elevation calculation.
	 */
	protected List<VectorXYZ> getBaseline() {
		return asList(getStartXYZ(), getEndXYZ());
	}

}
