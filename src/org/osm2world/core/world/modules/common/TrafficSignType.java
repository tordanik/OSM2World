package org.osm2world.core.world.modules.common;

import org.osm2world.core.target.common.material.Material;

public class TrafficSignType {
	
	public final Material material;	
	public final int numPosts;
	public final double defaultHeight;
	
	public TrafficSignType(Material material, int numPosts, double height) {
		this.material = material;
		this.numPosts = numPosts;
		this.defaultHeight = height;
		//System.out.println("new TraffSignType created: "+this.material.getTextureDataList().get(1).getFile().getName());
	}
}
