package org.osm2world.core.world.network;

import static java.util.Arrays.asList;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;

public abstract class VisibleConnectorNodeWorldObject<S extends NetworkWaySegmentWorldObject>
		extends NetworkNodeWorldObject<S> {

	protected boolean informationProvided;

	protected VectorXZ cutVector;
	protected VectorXZ startPos;
	protected VectorXZ endPos;
	protected float startWidth;
	protected float endWidth;

	/**
	 * returns the length required by this node representation.
	 * Adjacent lines will be pushed back accordingly.
	 *
	 * If this is 0, this has the same effect as an invisible
	 * connector node (adjacent line representations
	 * directly touching each other). Examples where non-zero values
	 * are needed include crossings at nodes in roads.
	 *
	 * Needs to be provided by the implementing class before the
	 * calculation in {@link NetworkCalculator} starts.
	 */
	abstract public float getLength();

	/**
	 * sets the results of {@link NetworkCalculator}'s calculations.
	 * Most methods in this class cannot be used until this method
	 * has provided the required information!
	 */
	void setInformation(VectorXZ cutVector,
			VectorXZ startPos, VectorXZ endPos,
			float startWidth, float endWidth) {

		this.informationProvided = true;

		this.cutVector = cutVector;
		this.startPos = startPos;
		this.endPos = endPos;
		this.startWidth = startWidth;
		this.endWidth = endWidth;

	}

	public VisibleConnectorNodeWorldObject(MapNode node, Class<S> segmentType) {
		super(node, segmentType);
	}

	@Override
	public SimplePolygonXZ getOutlinePolygonXZ() {

		checkInformationProvided();

		VectorXZ startRight = startPos.add(cutVector.mult(startWidth / 2));
		VectorXZ startLeft = startPos.add(cutVector.mult(-startWidth / 2));
		VectorXZ endRight = endPos.add(cutVector.mult(endWidth / 2));
		VectorXZ endLeft = endPos.add(cutVector.mult(-endWidth / 2));

		return new SimplePolygonXZ(asList(startRight, endRight, endLeft, startLeft, startRight));

	}

	/**
	 * throws an IllegalStateException if information hasn't been
	 * provided by a {@link NetworkCalculator}
	 */
	private void checkInformationProvided() throws IllegalStateException {
		if (!informationProvided) {
			throw new IllegalStateException("no connector information"
					+ " has been set for this representation.\n"
					+ "node: " + node);
		}
	}

}
