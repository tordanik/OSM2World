package org.osm2world.output.common.rendering;

/**
 * An orthographic projection.
 * With this kind of projection, objects do not change size depending on their distance to the camera.
 * A popular example would be "isometric" video game graphics.
 * You can use {@link OrthographicUtil} to more comfortably set up typical projections.
 *
 * @param aspectRatio   width / height
 * @param volumeHeight  height of the viewing volume; only relevant for <em>orthographic</em> projection
 */
public record OrthographicProjection(double aspectRatio, double volumeHeight,
			double nearClippingDistance, double farClippingDistance) implements Projection {

	public OrthographicProjection(double aspectRatio, double volumeHeight) {
		this(aspectRatio, volumeHeight, -10000, 10000);
	}

	public OrthographicProjection withVolumeHeight(double newVolumeHeight) {
		return new OrthographicProjection(aspectRatio, newVolumeHeight, nearClippingDistance, farClippingDistance);
	}

	public OrthographicProjection withAspectRatio(double newAspectRatio) {
		return new OrthographicProjection(newAspectRatio, volumeHeight, nearClippingDistance, farClippingDistance);
	}

	@Override
	public Projection withClippingDistances(double nearClippingDistance, double farClippingDistance) {
		return new OrthographicProjection(aspectRatio, volumeHeight, nearClippingDistance, farClippingDistance);
	}

	@Override
	public boolean orthographic() {
		return true;
	}

	public double volumeWidth() {
		return aspectRatio() * volumeHeight();
	}

}