package org.osm2world.core.world.network;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.SimplePolygonXZ;

/** junction between at least three segments in a network */
public abstract class JunctionNodeWorldObject<S extends NetworkWaySegmentWorldObject>
		extends NetworkNodeWorldObject<S> {

	/**
	 * describes the geometry of how one incoming way segment "interfaces" with the junction area.
	 * "Left" and "Right" are seen from the junction node's point of view.
	 */
	protected static class JunctionSegmentInterface {

		public final VectorXZ leftContactPos;
		public final VectorXZ rightContactPos;

		public JunctionSegmentInterface(VectorXZ leftContactPos, VectorXZ rightContactPos) {
			this.leftContactPos = leftContactPos;
			this.rightContactPos = rightContactPos;
		}

		@Override
		public String toString() {
			return asList(leftContactPos, rightContactPos).toString();
		}

	}

	protected List<JunctionSegmentInterface> segmentInterfaces = null;
	protected List<VectorXZ> pointsBetween;

	public JunctionNodeWorldObject(MapNode node, Class<S> segmentType) {
		super(node, segmentType);
	}

	/**
	 * sets the results of {@link NetworkCalculator}'s calculations.
	 *
	 * @param segmentInterfaces  How each segment connects to the junction area.
	 * Indices are the same as for {@link #getConnectedNetworkSegments()}.
	 * This kind of information will not be created for all way/area segments.
	 *
	 * @param pointsBetween  points to be inserted between the segment interfaces, can contain null entries.
	 * Point i should be inserted in the junction's outline after the interface for the i-th segment, unless it's null.
	 */
	void setInformation(List<JunctionSegmentInterface> segmentInterfaces, List<VectorXZ> pointsBetween) {
		if (segmentInterfaces.size() != pointsBetween.size() || segmentInterfaces.size() < 3) {
			throw new IllegalArgumentException();
		}
		this.segmentInterfaces = segmentInterfaces;
		this.pointsBetween = pointsBetween;
	}

	@Override
	public SimplePolygonXZ getOutlinePolygonXZ() {

		checkInformationProvided();

		List<VectorXZ> vectors = new ArrayList<>();

		for (int i = 0; i < segmentInterfaces.size(); i++) {

			JunctionSegmentInterface segmentInterface = segmentInterfaces.get(i);

			if (vectors.isEmpty() || !segmentInterface.leftContactPos.equals(vectors.get(vectors.size() - 1))) {
				vectors.add(segmentInterface.leftContactPos);
			}

			vectors.add(segmentInterface.rightContactPos);

			if (pointsBetween.get(i) != null) {
				vectors.add(pointsBetween.get(i));
			}

		}

		/* try to convert into a valid, counterclockwise simple polygon */

		// close polygon
		if (!vectors.get(vectors.size() - 1).equals(vectors.get(0))) {
			vectors.add(vectors.get(0));
		}

		SimplePolygonXZ simplePoly = new SimplePolygonXZ(vectors);

		return simplePoly.makeCounterclockwise();

	}

	/**
	 * throws an IllegalStateException if information hasn't been
	 * provided by a {@link NetworkCalculator}
	 */
	private void checkInformationProvided() throws IllegalStateException {
		if (segmentInterfaces == null) {
			throw new IllegalStateException("no junction information" +
					" has been set for " + this);
		}
	}

}
