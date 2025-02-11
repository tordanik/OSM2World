package org.osm2world.viewer.model;

import org.osm2world.target.common.rendering.OrthographicProjection;
import org.osm2world.target.common.rendering.PerspectiveProjection;

public final class Defaults {

	private Defaults() { }

	public static final PerspectiveProjection PERSPECTIVE_PROJECTION
		= new PerspectiveProjection(4/3.0, 50, 1, 100000);

	public static final OrthographicProjection ORTHOGRAPHIC_PROJECTION
		= new OrthographicProjection(4/3.0, 45, -10000, 10000);

}
