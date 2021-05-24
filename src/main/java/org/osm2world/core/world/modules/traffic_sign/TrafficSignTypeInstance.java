package org.osm2world.core.world.modules.traffic_sign;

import org.osm2world.core.target.common.material.Material;

/**
 * a single traffic sign. A {@link TrafficSignModel} has one or more of these.
 */
public class TrafficSignTypeInstance {

	public final Material material;
	public final int numPosts;
	public final double defaultHeight;

	public TrafficSignTypeInstance(Material material, int numPosts, double height) {
		this.material = material;
		this.numPosts = numPosts;
		this.defaultHeight = height;
	}

}
