package org.osm2world.core.world.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapSegment;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.NodeElevationProfile;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.PolygonXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.math.datastructures.IntersectionTestObject;
import org.osm2world.core.world.creation.NetworkCalculator;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.data.WorldObjectWithOutline;

public abstract class JunctionNodeWorldObject implements NodeWorldObject,
	IntersectionTestObject, WorldObjectWithOutline {

	protected final MapNode node;

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
	 * @param cutCenters  centers of the cuts to each;
	 *                    indices are the same as for the GridNode's
	 *                    {@link MapNode#getConnectedSegments()}
	 * @param cutVectors  vectors describing indicating the cut line,
	 *                    pointing to the right from the node's pov;
	 *                    for indices see junctionCutCenters
	 * @param widths      widths of the junction cut;
	 *                    for indices see junctionCutCenters
	 */
	public void setInformation(List<VectorXZ> cutCenters,
			List<VectorXZ> cutVectors, List<Float> widths) {

		this.informationProvided = true;

		this.cutCenters = cutCenters;
		this.cutVectors = cutVectors;
		this.widths = widths;

	}
	
	public JunctionNodeWorldObject(MapNode node) {
		this.node = node;
	}
	
	@Override
	public final MapElement getPrimaryMapElement() {
		return node;
	}
	
	/**
	 * provides outline for the areas covered by the junction.
	 * 
	 * The from and to indices refer to the list
	 * returned by the underlying {@link MapNode}'s
	 * {@link MapNode#getConnectedSegments()} method.
	 */
	public List<VectorXYZ> getOutline(int from, int to) {

		checkInformationProvided();
		
		List<VectorXYZ> outline = new ArrayList<VectorXYZ>();

		List<MapSegment> segments = node.getConnectedSegments();

		assert from >= 0 && from < segments.size();
		assert to >= 0 && to < segments.size();
			
		int i = from;
		
		while (i != to) {
			   
			VectorXZ newNodeA = getCutNode(i, false);
			if (newNodeA != null) {
				outline.add(node.getElevationProfile().getWithEle(newNodeA));
			}

			int nextI = i - 1;
			if (nextI < 0) { nextI = segments.size() - 1; }

			VectorXZ newNodeB = getCutNode(nextI, true);
			if (newNodeB != null) {
				outline.add(node.getElevationProfile().getWithEle(newNodeB));
			}

			i = nextI;

		}
		
		return outline;
		
	}

	@Override
	public PolygonXYZ getOutlinePolygon() {
		
		PolygonXYZ junctionArea = getJunctionArea();
		
		if (junctionArea == null) {
			return null;
		} else {
			
			try {
				
				SimplePolygonXZ simplePoly = junctionArea.getSimpleXZPolygon();
				
				if (simplePoly.isClockwise()) {
					return junctionArea.reverse();
				} else {
					return junctionArea;
				}
				
			} catch (InvalidGeometryException e) {
				//deal with non-simple polygons
				//TODO: this should be prevented from ever happening
				return null;
			}
			
		}
		
	}

	@Override
	public SimplePolygonXZ getOutlinePolygonXZ() {
		
		PolygonXYZ junctionArea = getJunctionArea();
		
		if (junctionArea == null) {
			return null;
		} else {
			
			try {
				
				SimplePolygonXZ simplePoly = junctionArea.getSimpleXZPolygon();
				
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
			
		}
		
	}
	
	@Override
	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
		if (getOutlinePolygon() != null) {
			return new AxisAlignedBoundingBoxXZ(getOutlinePolygon().getVertices());
		} else {
			return new AxisAlignedBoundingBoxXZ(
					node.getPos().x, node.getPos().z,
					node.getPos().x, node.getPos().z);
		}
	}
	
	@Override
	public double getClearingAbove(VectorXZ pos) {
		// current solution: maximum of connected segments' clearings.
		// Could probably find a more intelligent method.
		
		double max = 0;
		for (MapWaySegment waySegment : node.getConnectedWaySegments()) {
			WaySegmentWorldObject rep = waySegment.getPrimaryRepresentation();
			if (rep != null) {
				double clearing = rep.getClearingAbove(node.getPos());
				if (clearing > max) {
					max = clearing;
				}
			}
		}
		return max;
	}
	
	@Override
	public double getClearingBelow(VectorXZ pos) {
		// current solution: maximum of connected segments' clearings.
		// Could probably find a more intelligent method.
		
		double max = 0;
		for (MapWaySegment waySegment : node.getConnectedWaySegments()) {
			WaySegmentWorldObject rep = waySegment.getPrimaryRepresentation();
			if (rep != null) {
				double clearing = rep.getClearingBelow(node.getPos());
				if (clearing > max) {
					max = clearing;
				}
			}
		}
		return max;
	}

	/**
	 * @return  a triangulation of the area covered by this junction
	 */
	public Collection<TriangleXYZ> getTriangulation() {

		Collection<TriangleXZ> trianglesXZ = TriangulationUtil.triangulate(
				getOutlinePolygonXZ(),
				Collections.<SimplePolygonXZ>emptyList());
		
		Collection<TriangleXYZ> trianglesXYZ =
			new ArrayList<TriangleXYZ>(trianglesXZ.size());

		final NodeElevationProfile eleProfile = node.getElevationProfile();
		
		for (TriangleXZ triangleXZ : trianglesXZ) {
			VectorXYZ v1 = eleProfile.getWithEle(triangleXZ.v1);
			VectorXYZ v2 = eleProfile.getWithEle(triangleXZ.v2);
			VectorXYZ v3 = eleProfile.getWithEle(triangleXZ.v3);
			if (triangleXZ.isClockwise()) {
				trianglesXYZ.add(new TriangleXYZ(v3, v2, v1));
			} else  {
				trianglesXYZ.add(new TriangleXYZ(v1, v2, v3));
			}
		}
		
		return trianglesXYZ;
		
	}

	/**
	 * provides subclasses with an outline of the junction area.
	 * Can be null if the junction doesn't cover any area,
	 * which happens when there is only one connected way segment.
	 * 
	 * (Only available if junction information for this representation has been
	 * provided using {@link #setInformation(List, List, List)}.)
	 */
	protected PolygonXYZ getJunctionArea() {
				
		checkInformationProvided();

		List<VectorXZ> vectorsXZ = getJunctionAreaOutlineXZ();
		List<VectorXYZ> vectors = node.getElevationProfile().getWithEle(vectorsXZ);
		
		if (vectors.size() > 2) {
		
			vectors.add(vectors.get(0)); //close polygon
			
			return new PolygonXYZ(vectors);
			
		} else {
			
			return null;
			
		}
		
	}
	
	/**
	 * variant of {@link #getJunctionArea()} in the XZ plane
	 */
	protected PolygonXZ getJunctionAreaXZ() {
		
		checkInformationProvided();
		
		List<VectorXZ> vectors = getJunctionAreaOutlineXZ();
		
		if (vectors.size() > 2) {
		
			vectors.add(vectors.get(0)); //close polygon
			
			return new PolygonXZ(vectors);
			
		} else {
			
			return null;
			
		}
		
	}

	private List<VectorXZ> getJunctionAreaOutlineXZ() {
		
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
		return vectors;
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
	 * throws an IllegalStateException if information hasn't been
	 * provided by a {@link NetworkCalculator}
	 */
	private void checkInformationProvided() throws IllegalStateException {
		if (!informationProvided) {
			throw new IllegalStateException("no junction information" +
					" has been set for this representation");
		}
	}
		
	@Override
	public String toString() {
		return "junction node WO for " + node;
	}
	
}
