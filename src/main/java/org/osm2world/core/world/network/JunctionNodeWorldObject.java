package org.osm2world.core.world.network;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.PolygonXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;

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

	}

	protected List<JunctionSegmentInterface> segmentInterfaces = null;

	public JunctionNodeWorldObject(MapNode node, Class<S> segmentType) {
		super(node, segmentType);
	}

	/**
	 * sets the results of {@link NetworkCalculator}'s calculations.
	 *
	 * @param segmentInterfaces  How each segment connects to the junction area.
	 * Indices are the same as for {@link MapNode#getConnectedSegments()}.
	 * This kind of information will not be created for all way/area segments.
	 * The list can therefore contain null entries.
	 */
	void setInformation(List<JunctionSegmentInterface> segmentInterfaces) {
		this.segmentInterfaces = segmentInterfaces;
	}



	//TODO formerly @Override, currently useless
//	public double getClearingAbove(VectorXZ pos) {
//		// current solution: maximum of connected segments' clearings.
//		// Could probably find a more intelligent method.
//
//		double max = 0;
//		for (MapWaySegment waySegment : node.getConnectedWaySegments()) {
//			WaySegmentWorldObject rep = waySegment.getPrimaryRepresentation();
//			if (rep != null) {
//				double clearing = rep.getClearingAbove(node.getPos());
//				if (clearing > max) {
//					max = clearing;
//				}
//			}
//		}
//		return max;
//	}

	//TODO formerly @Override, currently useless
//	public double getClearingBelow(VectorXZ pos) {
//		// current solution: maximum of connected segments' clearings.
//		// Could probably find a more intelligent method.
//
//		double max = 0;
//		for (MapWaySegment waySegment : node.getConnectedWaySegments()) {
//			WaySegmentWorldObject rep = waySegment.getPrimaryRepresentation();
//			if (rep != null) {
//				double clearing = rep.getClearingBelow(node.getPos());
//				if (clearing > max) {
//					max = clearing;
//				}
//			}
//		}
//		return max;
//	}

	@Override
	public SimplePolygonXZ getOutlinePolygonXZ() {

		checkInformationProvided();

		List<VectorXZ> vectors = new ArrayList<>(segmentInterfaces.size() * 2);

		for (JunctionSegmentInterface segmentInterface : segmentInterfaces) {
			if (segmentInterface == null) continue; //TODO: this special case should be made obsolete
			vectors.add(segmentInterface.leftContactPos);
			vectors.add(segmentInterface.rightContactPos);
		}

		/* try to convert into a valid, counterclockwise simple polygon */

		if (vectors.size() > 2) {

			vectors.add(vectors.get(0)); //close polygon

			PolygonXZ poly = new PolygonXZ(vectors);

			try {

				SimplePolygonXZ simplePoly = poly.asSimplePolygon();

				if (simplePoly.isClockwise()) {
					return simplePoly.reverse();
				} else {
					return simplePoly;
				}

			} catch (InvalidGeometryException e) {
				//deal with non-simple polygons
				//TODO: this should be prevented from ever happening
				return null;
			}

		} else {

			return null;

		}

	}

	/**
	 * throws an IllegalStateException if information hasn't been
	 * provided by a {@link NetworkCalculator}
	 */
	private void checkInformationProvided() throws IllegalStateException {
		if (segmentInterfaces == null) {
			throw new IllegalStateException("no junction information" +
					" has been set for this representation");
		}
	}

}
