package org.osm2world.core.target.common.rendering;

/**
 * configuration of the projection from 3D world to screen
 * (<em>not</em> the map projection)
 */
public class Projection {

	private final boolean orthographic;

	/**
	 * width / height
	 */
	private final double aspectRatio;

	/**
	 * vertical viewing volume angle;
	 * only relevant for <em>perspective</em> projection
	 */
	private final double vertAngle;

	/**
	 * height of the viewing volume;
	 * only relevant for <em>orthographic</em> projection
	 */
	private final double volumeHeight;

	private final double nearClippingDistance;
	private final double farClippingDistance;

	public Projection(boolean orthographic, double aspectRatio,
			double vertAngle, double volumeHeight, double nearClippingDistance,
			double farClippingDistance) {
		this.orthographic = orthographic;
		this.aspectRatio = aspectRatio;
		this.vertAngle = vertAngle;
		this.volumeHeight = volumeHeight;
		this.nearClippingDistance = nearClippingDistance;
		this.farClippingDistance = farClippingDistance;
	}

	public boolean isOrthographic() {
		return orthographic;
	}

	public double getAspectRatio() {
		return aspectRatio;
	}

	public double getVertAngle() {
		return vertAngle;
	}

	public double getVolumeHeight() {
		return volumeHeight;
	}

	public double getNearClippingDistance() {
		return nearClippingDistance;
	}

	public double getFarClippingDistance() {
		return farClippingDistance;
	}

	public Projection withVolumeHeight(double newVolumeHeight) {
		return new Projection(orthographic, aspectRatio, vertAngle,
				newVolumeHeight, nearClippingDistance, farClippingDistance);
	}

	public Projection withAspectRatio(double newAspectRatio) {
		return new Projection(orthographic, newAspectRatio, vertAngle,
				volumeHeight, nearClippingDistance, farClippingDistance);
	}

	@Override
	public String toString() {
		return "{orthographic=" + orthographic
		+ ", aspectRatio=" + aspectRatio
		+ ", vertAngle=" + vertAngle
		+ ", volumeHeight=" + volumeHeight
		+ ", nearClippingDistance=" + nearClippingDistance
		+ ", farClippingDistance=" + farClippingDistance
		+ "}";
	}

}
