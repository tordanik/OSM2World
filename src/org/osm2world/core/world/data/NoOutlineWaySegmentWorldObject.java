package org.osm2world.core.world.data;

import static java.util.Arrays.asList;

import java.util.List;

import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IntersectionTestObject;

/**
 * superclass for {@link WaySegmentWorldObject}s that don't have an outline,
 * and are not part of a network.
 * Instead, they can be considered infinitely thin.
 */
public abstract class NoOutlineWaySegmentWorldObject
		implements WaySegmentWorldObject, IntersectionTestObject {
	
	protected final MapWaySegment segment;
	
	private final EleConnector startConnector;
	private final EleConnector endConnector;
	
	public NoOutlineWaySegmentWorldObject(MapWaySegment segment) {
		
		this.segment = segment;
		
		startConnector = new EleConnector(getStartPosition());
		endConnector = new EleConnector(getEndPosition());
		
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
	public VectorXZ getStartPosition() {
		return segment.getStartNode().getPos();
	}
	
	@Override
	public VectorXZ getEndPosition() {
		return segment.getEndNode().getPos();
	}
	
	@Override
	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
		return new AxisAlignedBoundingBoxXZ(asList(
				getStartPosition(), getEndPosition()));
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
