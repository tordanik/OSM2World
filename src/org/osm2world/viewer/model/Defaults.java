package org.osm2world.viewer.model;

import org.osm2world.core.target.common.rendering.Projection;

public final class Defaults {
	
	private Defaults() { }
	
	public static final Projection PERSPECTIVE_PROJECTION 
		= new Projection(false, 1, 45, 50, 1, 10000);

	public static final Projection ORTHOGRAPHIC_PROJECTION 
		= new Projection(true, 1, 45, 50, -100000, 100000);
	
}
