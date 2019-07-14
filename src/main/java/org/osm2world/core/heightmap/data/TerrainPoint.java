package org.osm2world.core.heightmap.data;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

public class TerrainPoint {
	
	private final VectorXZ pos;
	private Float ele;
	
	public TerrainPoint(VectorXZ pos, Float ele) {
		this.pos = pos;
		this.ele = ele;
	}
	
	public VectorXZ getPos() {
		return pos;
	}
	
	public VectorXYZ getPosXYZ() {
		return pos.xyz(ele);
	}
	
	/**
	 * returns the point's elevation;
	 * null indicates an unknown elevation
	 */
	public Float getEle() {
		return ele;
	}
	
	/**
	 * sets the point's elevation;
	 * null indicates an unknown elevation
	 */
	public void setEle(float ele) {
		this.ele = ele;
	}
	
	@Override
	public String toString() {
		return "(" + pos + "," + ele + ")";
	}
	
}
