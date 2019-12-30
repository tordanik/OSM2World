package org.osm2world.core.target.jogl;

import java.awt.Color;
import java.io.File;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;

public interface JOGLTarget extends Target {

	/**
	 * discards all accumulated draw calls
	 */
	public void reset();

	public void drawPoints(Color color, VectorXYZ... vs);

	public void drawLineStrip(Color color, int width, VectorXYZ... vs);

	public void drawLineStrip(Color color, int width, List<VectorXYZ> vs);

	public void drawLineLoop(Color color, int width, List<VectorXYZ> vs);

	/**
	 * set global lighting parameters. Using this method affects all primitives
	 * (even those from previous draw calls).
	 *
	 * @param parameters  parameter object; null disables lighting
	 */
	public void setGlobalLightingParameters(
			GlobalLightingParameters parameters);

	/**
	 * set global rendering parameters. Using this method affects all primitives
	 * (even those from previous draw calls).
	 */
	public void setRenderingParameters(
			JOGLRenderingParameters renderingParameters);

	public void setConfiguration(Configuration config);

	public boolean isFinished();

	public void render(Camera camera, Projection projection);

	/**
	 * similar to {@link #render(Camera, Projection)},
	 * but allows rendering only a part of the "normal" image.
	 * For example, with xStart=0, xEnd=0.5, yStart=0 and yEnd=1,
	 * only the left half of the full image will be rendered,
	 * but it will be stretched to cover the available space.
	 *
	 * Only supported for orthographic projections!
	 */
	public void renderPart(Camera camera, Projection projection,
			double xStart, double xEnd, double yStart, double yEnd);

	public void freeResources();

	public void drawBackgoundImage(File backgroundImage,
			int startPixelX, int startPixelY,
			int pixelWidth, int pixelHeight,
			JOGLTextureManager textureManager);

	/**
	 * Set the boundary for the relevant data. All data outside of this boundary may be ignored by the target
	 * to improve quality and performance.
	 */
	public void setXZBoundary(AxisAlignedBoundingBoxXZ boundary);

}
