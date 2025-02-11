package org.osm2world.target.common.rendering;

/**
 * A perspective projection.
 * This kind of projection produces a true 3D effect by making objects further from the camera appear smaller.
 *
 * @param aspectRatio  width / height
 * @param vertAngle    vertical viewing volume angle; only relevant for <em>perspective</em> projection
 */
public record PerspectiveProjection(double aspectRatio, double vertAngle,
			double nearClippingDistance, double farClippingDistance) implements Projection {

	public PerspectiveProjection(double aspectRatio, double vertAngle) {
		this(aspectRatio, vertAngle, 1, 50000);
	}

	public PerspectiveProjection withAspectRatio(double newAspectRatio) {
		return new PerspectiveProjection(newAspectRatio, vertAngle,
				nearClippingDistance, farClippingDistance);
	}

	@Override
	public Projection withClippingDistances(double nearClippingDistance, double farClippingDistance) {
		return new PerspectiveProjection(aspectRatio, vertAngle, nearClippingDistance, farClippingDistance);
	}

	@Override
	public boolean orthographic() {
		return false;
	}

}