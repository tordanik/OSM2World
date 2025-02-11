package org.osm2world.target.common.rendering;

/**
 * configuration of the projection from 3D world to screen
 * (<em>not</em> the map projection).
 */
public interface Projection {

	/** whether this is an {@link OrthographicProjection} */
	boolean orthographic();

	/** width / height */
	double aspectRatio();

	double nearClippingDistance();

	double farClippingDistance();

	Projection withAspectRatio(double aspectRatio);

	Projection withClippingDistances(double nearClippingDistance, double farClippingDistance);

}
