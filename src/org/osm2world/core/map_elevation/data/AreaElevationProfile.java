package org.osm2world.core.map_elevation.data;

import java.util.ArrayList;
import java.util.Collection;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_elevation.creation.ElevationCalculator;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

import com.google.common.base.Function;

/**
 * elevation profile for a {@link MapArea}
 */
public class AreaElevationProfile extends ElevationProfile {

	private final MapArea area;
	
	private Collection<VectorXYZ> pointsWithEle = null;

	private Function<VectorXZ, VectorXYZ> eleFunction;
	
	Collection<TriangleXYZ> triangulation = null;
	
	public AreaElevationProfile(MapArea area) {
		super();
		this.area = area;
	}
	
	@Override
	protected MapElement getElement() {
		return area;
	}

	/**
	 * returns all points on the area where elevation values exist.
	 * This will at least include the outline points.
	 * Points are not sorted in any way. Must not be used before calculation
	 * results have been set using {@link #addPointWithEle(VectorXYZ)}
	 */
	@Override
	public Collection<VectorXYZ> getPointsWithEle() {
		
		if (pointsWithEle == null) {
			throw new IllegalStateException("elevations have not been calculated yet");
		} else if (pointsWithEle.size() < 2) {
			throw new IllegalStateException("an area must have at least two points with elevation");
		}
		
		return pointsWithEle;
	}
	
	@Override
	public double getEleAt(final VectorXZ pos) {
		
//		if (triangulation == null) {
//			calculateTriangulation();
//		}
		//TODO: calculate correctly and with better performance.
		//Should be possible using a triangulation.
		
		//temporary solution: find closest point with ele
				
//		VectorXYZ closestPoint = Collections.min(pointsWithEle, new Comparator<VectorXYZ>(){
//			public int compare(VectorXYZ p1, VectorXYZ p2) {
//				return Double.compare(
//						p1.xz().distanceTo(pos),
//						p2.xz().distanceTo(pos));
//			};
//		});
//
//		return closestPoint.y;
		
		return eleFunction.apply(pos).y;
		
	}
	
	@Override
	public VectorXYZ getWithEle(VectorXZ pos) {
		//TODO keep in sync with getEleAt
		return eleFunction.apply(pos);
	}
			
	/**
	 * adds a result of {@link ElevationCalculator}.
	 * Must be called at least once for every outline node
	 */
	public void addPointWithEle(VectorXYZ pointWithEle) {
		
		if (pointsWithEle == null) {
			pointsWithEle = new ArrayList<VectorXYZ>();
		}
		
		this.pointsWithEle.add(pointWithEle);
		
	}
	
	public void setEleFunction(Function<VectorXZ, VectorXYZ> eleFunction) {
		this.eleFunction = eleFunction;
	}

	@Override
	public double getMaxEle() {
		if (pointsWithEle == null) {
			throw new IllegalStateException("elevations have not been calculated yet");
		}
		double maxEle = Double.MIN_VALUE;
		for (VectorXYZ pointWithEle : pointsWithEle) {
			maxEle = Math.max(maxEle, pointWithEle.y);
		}
		return maxEle;
	}

	@Override
	public double getMinEle() {
		if (pointsWithEle == null) {
			throw new IllegalStateException("elevations have not been calculated yet");
		}
		double minEle = Double.MAX_VALUE;
		for (VectorXYZ pointWithEle : pointsWithEle) {
			minEle = Math.min(minEle, pointWithEle.y);
		}
		return minEle;
	}
	
//  TODO: finish implementation
//	/**
//	 * returns a triangulation of the associated area
//	 * (the one returned by {@link #getElement()}),
//	 * with elevation information
//	 */
//	public Collection<TriangleXYZ> getTriangulation() {
//		return triangulation;
//	}
//
//	/**
//	 * calculates a triangulation of this area
//	 * and writes the result to {@link #triangulation}
//	 */
//	private void calculateTriangulation() {
//
//		if (pointsWithEle == null) {
//			throw new IllegalStateException("elevations have not been calculated yet");
//		} else if (pointsWithEle.size() < 2) {
//			throw new IllegalStateException("an area must have at least two points with elevation");
//		}
//
//		Collection<TriangleXZ> trianglesXZ =
//			TriangulationUtil.triangulate(area.getPolygon());
//
//		Collection<TriangleXYZ> trianglesXYZ =
//			new ArrayList<TriangleXYZ>(trianglesXZ.size());
//
//		for (TriangleXZ triangleXZ : trianglesXZ) {
//			VectorXYZ v1 = eleProfile.getWithEle(triangleXZ.v1);
//			VectorXYZ v2 = eleProfile.getWithEle(triangleXZ.v2);
//			VectorXYZ v3 = eleProfile.getWithEle(triangleXZ.v3);
//			if (triangleXZ.isClockwise()) {
//				trianglesXYZ.add(new TriangleXYZ(v3, v2, v1));
//			} else  {
//				trianglesXYZ.add(new TriangleXYZ(v1, v2, v3));
//			}
//		}
//
//	}
	
}
