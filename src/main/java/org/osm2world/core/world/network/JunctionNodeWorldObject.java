package org.osm2world.core.world.network;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapSegment;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.PolygonXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.world.creation.NetworkCalculator;

public abstract class JunctionNodeWorldObject<S extends NetworkWaySegmentWorldObject>
		extends NetworkNodeWorldObject<S> {

	protected boolean informationProvided = false;
	protected List<VectorXZ> cutVectors;
	protected List<VectorXZ> cutCenters;
	protected List<Float> widths;

	/**
	 * sets the results of {@link NetworkCalculator}'s calculations.
	 *
	 * Cut information will not be created for all way/area segments.
	 * The lists can therefore contain null entries.
	 *
	 * @param cutCenters
	 * centers of the cuts to each;
	 * indices are the same as for the GridNode's {@link MapNode#getConnectedSegments()}
	 * @param cutVectors
	 * vectors describing indicating the cut line,
	 * pointing to the right from the node's pov;
	 * for indices see junctionCutCenters
	 * @param widths
	 * widths of the junction cut;
	 * for indices see junctionCutCenters
	 */
	public void setInformation(List<VectorXZ> cutCenters,
			List<VectorXZ> cutVectors, List<Float> widths) {

		this.informationProvided = true;

		this.cutCenters = cutCenters;
		this.cutVectors = cutVectors;
		this.widths = widths;

	}

	public JunctionNodeWorldObject(MapNode node, Class<S> segmentType) {
		super(node, segmentType);
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

		List<VectorXZ> vectors = new ArrayList<VectorXZ>(cutCenters.size()*2+1);

		for (int i=0; i < cutCenters.size(); i++) {

			if (cutCenters.get(i) == null) continue;

			VectorXZ left = getCutNode(i, false);
			VectorXZ right = getCutNode(i, true);

			if (left != null) {
				vectors.add(left);
			}
			if (right != null) {
				vectors.add(right);
			}

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
	 * calculates the left or right node of a cut
	 * (Only available if junction information for this representation has been
	 * provided using {@link #setInformation(List, List, List)}).
	 *
	 * @return cut node position; null if connected section #i has no outline
	 */
	protected VectorXZ getCutNode(int i, boolean right) {

		checkInformationProvided();

		VectorXZ cutCenter = cutCenters.get(i);
		VectorXZ cutVector = cutVectors.get(i);
		Float width = widths.get(i);

		if (cutCenter == null) {
			return null;
		} else {

			if (right) {
				return cutCenter.add(cutVector.mult(width * 0.5f));
			} else {
				return cutCenter.subtract(cutVector.mult(width * 0.5f));
			}

		}

	}

	/**
	 * provides outline for the areas covered by the junction.
	 *
	 * The from and to indices refer to the list
	 * returned by the underlying {@link MapNode}'s
	 * {@link MapNode#getConnectedSegments()} method.
	 */
	public List<VectorXZ> getOutline(int from, int to) {

		checkInformationProvided();

		List<VectorXZ> outline = new ArrayList<VectorXZ>();

		List<MapSegment> segments = node.getConnectedSegments();

		assert from >= 0 && from < segments.size();
		assert to >= 0 && to < segments.size();

		int i = from;

		while (i != to) {

			VectorXZ newNodeA = getCutNode(i, false);
			if (newNodeA != null) {
				outline.add(newNodeA);
			}

			int nextI = i - 1;
			if (nextI < 0) { nextI = segments.size() - 1; }

			VectorXZ newNodeB = getCutNode(nextI, true);
			if (newNodeB != null) {
				outline.add(newNodeB);
			}

			i = nextI;

		}

		return outline;

	}

	/**
	 * throws an IllegalStateException if information hasn't been
	 * provided by a {@link NetworkCalculator}
	 */
	private void checkInformationProvided() throws IllegalStateException {
		if (!informationProvided) {
			throw new IllegalStateException("no junction information" +
					" has been set for this representation");
		}
	}

}
