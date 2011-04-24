package org.osm2world.core.map_elevation.data;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.creation.ElevationCalculator;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

/**
 * elevation profile for a {@link MapWaySegment}
 */
public class WaySegmentElevationProfile extends ElevationProfile {

	private final MapWaySegment line;

	private List<VectorXYZ> pointsWithEle = null;

	public WaySegmentElevationProfile(MapWaySegment line) {
		this.line = line;
	}

	@Override
	protected MapElement getElement() {
		return line;
	}
	
	//TODO: is this needed?
	/** sort pointsWithEle by ascending (squared) distance from startPos */
	private void sortPointsWithEle() {
		Collections.sort(pointsWithEle, new Comparator<VectorXYZ>() {
			final VectorXZ startPos = line.getPrimaryRepresentation().getStartPosition();
			@Override
			public int compare(VectorXYZ v1, VectorXYZ v2) {
				return Double.compare(v1.xz().subtract(startPos)
						.lengthSquared(), v2.xz().subtract(startPos)
						.lengthSquared());
			}
		});
	}
	
	/*
	 * methods providing access to the results
	 */

	/**
	 * returns all points along the line where elevation values exist. This will
	 * at least include the starting and end point of the line. Other elements
	 * in between are ordered along the line. Must not be used before calculation
	 * results have been set using {@link #addPointWithEle(VectorXYZ)}
	 */
	@Override
	public List<VectorXYZ> getPointsWithEle() {
		
		if (pointsWithEle == null) {
			throw new IllegalStateException("elevations have not been calculated yet");
		} else if (pointsWithEle.size() < 2) {
			throw new IllegalStateException("a line must have at least two points with elevation");
		}
		
		return pointsWithEle;
	}

	@Override
	public double getEleAt(VectorXZ pos) {

		if (pointsWithEle == null) {
			throw new IllegalStateException("elevations have not been calculated yet");
		} else if (pointsWithEle.size() < 2) {
			throw new IllegalStateException("a line must have at least two points with elevation");
		}
		
		//TODO: start pos isn't identical with first pointWithEle! can this cause problems?
		
		VectorXZ startPos = line.getPrimaryRepresentation().getStartPosition();
		
		final double posDistance = pos.subtract(startPos).length();

		// find points with known elevation directly before and after pos

		VectorXYZ before = null;
		VectorXYZ after = null;

		double beforeDistance = 0;
		double afterDistance = 0;

		for (VectorXYZ v : pointsWithEle) {
			double vFraction = v.xz().subtract(startPos).length();
			if (vFraction < posDistance) {
				before = v;
				beforeDistance = vFraction;
			} else if (vFraction == posDistance) {
				return v.y;
			} else {
				after = v;
				afterDistance = vFraction;
				break; // pointsWithEle are ordered
			}
		}

		// handle pos outside [startPos; endPos]

		if (before == null) {
			return after.y;
		} else if (after == null) {
			return before.y;
		}

		// interpolate between points before and after
		
		double influenceOfAfter = (posDistance - beforeDistance)
				/ (afterDistance - beforeDistance);

		double ele = before.y * (1-influenceOfAfter) + after.y * influenceOfAfter;

		return ele;

	}
	
	/**
	 * adds a result of {@link ElevationCalculator}.
	 * Must be called at least twice (start and end node)
	 */
	public void addPointWithEle(VectorXYZ pointWithEle) {
		
		if (pointsWithEle == null) {
			pointsWithEle = new ArrayList<VectorXYZ>();
		}
		
		this.pointsWithEle.add(pointWithEle);
		
		sortPointsWithEle(); //TODO: (performance) don't do this every time; insert in the right place instead
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
	
}
