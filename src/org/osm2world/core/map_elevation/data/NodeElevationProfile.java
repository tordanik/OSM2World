package org.osm2world.core.map_elevation.data;

import java.util.Arrays;
import java.util.Collection;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;



/**
 * elevation profile for a {@link MapNode}
 */
public class NodeElevationProfile extends ElevationProfile {

	private final MapNode node;
	
	private VectorXYZ pointWithEle;

	public NodeElevationProfile(MapNode node) {
		this.node = node;
	}
	
	@Override
	protected MapElement getElement() {
		return node;
	}
		
	public VectorXYZ getPointWithEle() {
		if (pointWithEle == null) {
			throw new IllegalStateException("elevations have not been calculated yet");
		}
		return pointWithEle;
	}
	
	@Override
	public Collection<VectorXYZ> getPointsWithEle() {
		return Arrays.asList(pointWithEle);
	}
		
	public double getEle() {
		if (pointWithEle == null) {
			throw new IllegalStateException("elevations have not been calculated yet");
		}
		return pointWithEle.y;
	}
	
	@Override
	public double getEleAt(VectorXZ pos) {
		return getEle();
	}

	public void setEle(double ele) {
		pointWithEle = node.getPos().xyz(ele);
	}
	
	@Override
	public double getMaxEle() {
		if (pointWithEle == null) {
			throw new IllegalStateException("elevations have not been calculated yet");
		}
		return pointWithEle.y;
	}
	
	@Override
	public double getMinEle() {
		if (pointWithEle == null) {
			throw new IllegalStateException("elevations have not been calculated yet");
		}
		return pointWithEle.y;
	}
	
}
